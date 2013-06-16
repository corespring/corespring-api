function HomeController($scope, $timeout, $rootScope, $http, $location, ItemService, SearchService, Collection, Contributor, ItemFormattingUtils, Organization) {

  //Mixin ItemFormattingUtils
  angular.extend($scope, ItemFormattingUtils);

  $http.defaults.headers.get = ($http.defaults.headers.get || {});
  $http.defaults.headers.get['Content-Type'] = 'application/json';

  $scope.$root.mode = "home";

  $scope.searchParams = $rootScope.searchParams ? $rootScope.searchParams : ItemService.createWorkflowObject();
  $rootScope.$broadcast('onListViewOpened');

  var init = function () {
    loadOrganization();
    loadCollectionsAndSearch();
    loadContributors();
    $scope.showDraft = true;

    var defaultsFactory = new com.corespring.model.Defaults();
    $scope.gradeLevelDataProvider = defaultsFactory.buildNgDataProvider("gradeLevels");
    $scope.itemTypeDataProvider = defaultsFactory.buildNgDataProvider("itemTypes");
    $scope.flatItemTypeDataProvided = _.map(_.flatten(_.pluck($scope.itemTypeDataProvider, 'label')), function(e) {
      return {key: e, label: e};
    });
    $scope.flatItemTypeDataProvided.push({key: "Other", label: "Other"});
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

  $scope.sortBy = function(field) {
    if ($scope.searchParams.sort && $scope.searchParams.sort[field]) {
      $scope.searchParams.sort[field] *= -1;
    } else {
      $scope.searchParams.sort = {};
      $scope.searchParams.sort[field] = 1;
    }
    $scope.$broadcast("sortingOnField", field, $scope.searchParams.sort[field] == 1);
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
    var out = _.map(items, function (i) {
      return i.name
    });
    return out.join(", ").replace(/CoreSpring/g, "");
  };

  $scope.getSelectedTitle = function (items) {
    if (!items || items.length == 0) {
      return "None Selected";
    }
    var out = _.map(items, function (i) {
      return i.key
    });
    return out.join(", ").replace(/0/g, "");
  };

  $scope.search = function () {
    var isOtherSelected = $scope.searchParams && _.find($scope.searchParams.itemType, function (e) {
      return e.label == "Other"
    });

    if (isOtherSelected) {
      $scope.searchParams.notSelectedItemTypes = [];
      _.each($scope.flatItemTypeDataProvided, function (e) {
        var isSelected = _.find($scope.searchParams.itemType, function (f) {
          return e.label == f.label;
        });
        if (!isSelected)
          $scope.searchParams.notSelectedItemTypes.push(e);
      });
    }
    SearchService.search($scope.searchParams, function (res) {
      $rootScope.items = _.map(res, function(item){
        var readOnlyColl = _.find($rootScope.collections, function(coll){
            return (coll.id == item.collectionId) && (coll.access == 1)  //1 represents read-only access
        })
        if(readOnlyColl){
            item.readOnly = true;
        }else{
            item.readOnly = false;
        }
        return item;
      })
      setTimeout(function () {
        MathJax.Hub.Queue(["Typeset", MathJax.Hub]);
      }, 200);
    });
  };

  $scope.loadMore = function () {
    SearchService.loadMore(function () {
        // re-bind the scope collection to the services model after result comes back
        $rootScope.items = SearchService.itemDataCollection;
        //Trigger MathJax
        setTimeout(function () {
          MathJax.Hub.Queue(["Typeset", MathJax.Hub]);
        }, 200);

      }
    );
  };

  function loadCollectionsAndSearch() {
    Collection.get({}, function (data) {
        $rootScope.collections = data;
        //all collections with write access will be selected by default
        $scope.searchParams.collection = _.filter(data, function(coll){
            return (coll.access & 3) == 3
        })
        $scope.search();
      },
      function () {
        console.log("load collections: error: " + arguments);
      });
  }

  function loadContributors() {
    Contributor.get({}, function (data) {
        $scope.contributors = data;
      },
      function () {
        console.log("load contributors: error: " + arguments);
      });
  }
  function loadOrganization(){
    Organization.get({path: "default"}, function(data){
        $scope.orgName = data.name;
        $scope.isRoot = true; //always show
    })
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
    console.log("Item Delete Cancelled");
    $scope.itemToDelete = null;
    $scope.showConfirmDestroyModal = false;
  };

  $scope.itemClick = function(){
    if(this.item.readOnly){
        console.log(this.item)
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

  $scope.publishStatus = function(isPublished){
    if(isPublished) return "Published"
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
        //console.log(err)
    }
  };

  if (window.addEventListener) {
    window.addEventListener('message', fn, true);
  }else if (window.attachEvent) {
    window.attachEvent('message', fn);
  }
  //////

  init();
}

HomeController.$inject = ['$scope',
  '$timeout',
  '$rootScope',
  '$http',
  '$location',
  'ItemService',
  'SearchService',
  'Collection',
  'Contributor',
  'ItemFormattingUtils',
  'Organization'];

