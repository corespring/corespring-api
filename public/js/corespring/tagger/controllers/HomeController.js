function HomeController($scope, $timeout, $rootScope, $http, $location, ItemService, SearchService, V2SearchService, CollectionManager, Contributor, ItemFormattingUtils, Logger) {

  //Mixin ItemFormattingUtils
  angular.extend($scope, ItemFormattingUtils);


  $http.defaults.headers.get = ($http.defaults.headers.get || {});
  $http.defaults.headers.get['Content-Type'] = 'application/json';

  $scope.$root.mode = "home";


  $rootScope.searchParams = $rootScope.searchParams ? $rootScope.searchParams : ItemService.createWorkflowObject();
  $rootScope.$broadcast('onListViewOpened');

  var init = function () {
    loadCollections();
    loadContributors();
    $scope.showDraft = true;

    var defaultsFactory = new com.corespring.model.Defaults();
    $scope.gradeLevelDataProvider = defaultsFactory.buildNgDataProvider("gradeLevels");
    $scope.itemTypeDataProvider = defaultsFactory.buildNgDataProvider("itemTypes");
    $scope.statuses = [
      {label: "Setup", key: "setup"},
      {label: "Tagged", key: "tagged"},
      {label: "Standards Aligned", key: "standardsAligned"},
      {label: "QA Review", key: "qaReview"},
      {label: "Exact Match", key: "exactMatch"}
    ];
    $scope.publishStatuses = [
      {label: "Published", key: "published"},
      {label: "Draft", key: "draft"}
    ]
  };

  $scope.sortBy = function (field) {
    if ($rootScope.searchParams.sort && $rootScope.searchParams.sort[field]) {
      $rootScope.searchParams.sort[field] *= -1;
    } else {
      $rootScope.searchParams.sort = {};
      $rootScope.searchParams.sort[field] = 1;
    }
    $scope.$broadcast("sortingOnField", field, $rootScope.searchParams.sort[field] == 1);
    $scope.search();
  }

  $scope.getContributorTitle = function (c) {
    return c.name;
  };

  $scope.getContributorSelectedTitle = function (items) {
    if (!items || items.length == 0) {
      return "None Selected";
    }
    return items.length + " Selected";
  }

  $scope.getCollectionTitle = function (c) {
    return c.name.replace("CoreSpring", "");
  };

  $scope.getTitle = function (o) {
    return o.key.replace(/^0/, "")
  };
  $scope.getLabel = function (o) {
    return o.label
  };

  $scope.getCollectionSelectedTitle = function (items) {
    if (!items || items.length == 0) {
      return "None Selected";
    }
    return items.length + " selected";
  };

  $scope.getSelectedTitle = function (items) {
    if (!items || items.length == 0) {
      return "None Selected";
    }
    var out = _.pluck(items, "key").map(function (key) {
      var numericKey = parseInt(key)
      return isNaN(numericKey) ? key : numericKey
    });
    return out.join(", ")
  };

  function applyPermissions(items){
    var readOnlyCollections = _.filter(CollectionManager.rawCollections,function(c){
      return c.permission == "read";
    });
     return _.map(items, function(item){
        var readOnlyColl = _.find(readOnlyCollections, function(coll){
            return coll.id == item.collectionId;  //1 represents read-only access
        });
        if(readOnlyColl){
            item.readOnly = true;
        }else{
            item.readOnly = false;
        }
        return item;
      });
  }

  $scope.lazySearch = _.debounce(function() {
    $scope.search();
    $scope.$apply();
  }, 500);

  $scope.search = function () {
    var isOtherSelected = $rootScope.searchParams && _.find($rootScope.searchParams.itemType, function (e) {
      return e.label == "Other"
    });

    if (isOtherSelected) {
      $rootScope.searchParams.notSelectedItemTypes = [];
      _.each($scope.flatItemTypeDataProvided, function (e) {
        var isSelected = _.find($rootScope.searchParams.itemType, function (f) {
          return e.label == f.label;
        });
        if (!isSelected)
          $rootScope.searchParams.notSelectedItemTypes.push(e);
      });
    }
    V2SearchService.search($rootScope.searchParams, function (res) {
      $rootScope.items = applyPermissions(res);
      setTimeout(function () {
        MathJax.Hub.Queue(["Typeset", MathJax.Hub]);
      }, 200);
    });
  };

  $scope.loadMore = function () {
    V2SearchService.loadMore(function(results) {
        // re-bind the scope collection to the services model after result comes back
        $rootScope.items = applyPermissions(results);
        //Trigger MathJax
        setTimeout(function () {
          MathJax.Hub.Queue(["Typeset", MathJax.Hub]);
        }, 200);

      }
    );
  };

  function loadCollections() {

    $scope.$watch( function(){ return CollectionManager.sortedCollections; }, function(newValue, oldValue){
      if (newValue) {
        $scope.sortedCollections = newValue;
        if (!$rootScope.searchParams.collection && $scope.sortedCollections) {
          $rootScope.searchParams.collection = _.clone($scope.sortedCollections[0].collections);
        }
        $scope.search();
      }
    }, true);

    CollectionManager.init();
  }

  function loadContributors() {
    Contributor.get({}, function (data) {
        $scope.contributors = data;
      },
      function (err) {
          console.log("error occurred when loading contributors: "+JSON.stringify(e));
      });
  }

  $scope.showGradeLevel = function () {
    return $scope.createGradeLevelString(this.item.gradeLevel);
  };


  $scope.deleteItem = function (item) {
    $scope.itemToDelete = item;
    $scope.showConfirmDestroyModal = true;
  };

  $scope.deleteConfirmed = function () {
    var deletingId = $scope.itemToDelete.id;
    ItemService.remove({id: $scope.itemToDelete.id},
      function (result) {
        $scope.itemToDelete = null;
        $scope.search();
      }
    );
    $scope.itemToDelete = null;
    $scope.showConfirmDestroyModal = false;
  };

  $scope.deleteCancelled = function () {
    $scope.itemToDelete = null;
    $scope.showConfirmDestroyModal = false;
  };

  $scope.itemClick = function(){
    if(this.item.readOnly){
        $scope.openItem(this.item.id)
    }else{
      SearchService.currentItem = this.item;
      $location.url('/edit/' + this.item.id + "?panel=metadata");
    }
  }
  /*
   * called from the repeater. scope (this) is the current item
   */
  $scope.openEditView = function () {
    SearchService.currentItem = this.item;
    $location.url('/edit/' + this.item.id + "?panel=metadata");
  };

  $scope.publishStatus = function (isPublished) {
    if (isPublished) return "Published"
    else return "Draft"
  }

  //from items-app.js
  $scope.hidePopup = function() {
    $scope.showPopup = false;
    $scope.previewingId = "";
    $scope.popupBg="";
  };

  $scope.openItem = function (id) {
    $timeout(function () {
      $scope.showPopup = true;
      $scope.popupBg = "extra-large-window"
      $scope.previewingId = id;
      //$scope.$broadcast("requestLoadItem", id);
      $('#preloader').show();
      $('#player').hide();
    }, 50);
    $timeout(function () {
      $('.window-overlay').scrollTop(0);
    }, 100);

  };

  $scope.onItemLoad = function () {
    $('#preloader').hide();
    $('#player').show();
  };

  var fn = function(m) {
    try{
      var data = JSON.parse(m.data);
      if (data.message == 'closeProfilePopup') {
        $timeout(function() {
          $scope.hidePopup();
        }, 10);
      }
    }catch(err){
       //it is normal for this error to be thrown
       // Logger.error("Error occurred on window event listener in home controller: "+JSON.stringify(err));
    }
  };

  if (window.addEventListener) {
    window.addEventListener('message', fn, true);
  }else if (window.attachEvent) {
    window.attachEvent('message', fn);
  }

  init();
}

HomeController.$inject = ['$scope',
  '$timeout',
  '$rootScope',
  '$http',
  '$location',
  'ItemService',
  'SearchService',
  'V2SearchService',
  'CollectionManager',
  'Contributor',
  'ItemFormattingUtils',
  'Logger'
  ];
