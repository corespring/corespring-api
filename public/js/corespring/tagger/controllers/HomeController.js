function HomeController($scope, $rootScope, $http, $location, ItemService, SearchService, Collection, Contributor, ItemFormattingUtils) {

  //Mixin ItemFormattingUtils
  angular.extend($scope, ItemFormattingUtils);

  $http.defaults.headers.get = ($http.defaults.headers.get || {});
  $http.defaults.headers.get['Content-Type'] = 'application/json';

  $scope.$root.mode = "home";

  $scope.searchParams = $rootScope.searchParams ? $rootScope.searchParams : ItemService.createWorkflowObject();
  $rootScope.$broadcast('onListViewOpened');


  var init = function () {

    $scope.search();
    loadCollections();
    loadContributors();

    var defaultsFactory = new com.corespring.model.Defaults();
    $scope.gradeLevelDataProvider = defaultsFactory.buildNgDataProvider("gradeLevels");
    $scope.itemTypeDataProvider = defaultsFactory.buildNgDataProvider("itemTypes");
    $scope.flatItemTypeDataProvied = _.map(_.flatten(_.pluck($scope.itemTypeDataProvider, 'label')), function(e) {
      return {key: e, label: e};
    });
    $scope.flatItemTypeDataProvied.push({key: "Other", label: "Other"});
    $scope.statuses = [
      {label: "Setup", key: "setup"},
      {label: "Tagged", key: "tagged"},
      {label: "Standards Aligned", key: "standardsAligned"},
      {label: "QA Review", key: "qaReview"},
      {label: "Exact Match", key: "exactMatch"}
    ];
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
      _.each($scope.flatItemTypeDataProvied, function (e) {
        var isSelected = _.find($scope.searchParams.itemType, function (f) {
          return e.label == f.label;
        });
        if (!isSelected)
          $scope.searchParams.notSelectedItemTypes.push(e);
      });
    }
    SearchService.search($scope.searchParams, function (res) {
      $rootScope.items = res;
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


  function loadCollections() {
    Collection.get({}, function (data) {
        $scope.collections = data;
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


  /*
   * called from the repeater. scope (this) is the current item
   */
  $scope.openEditView = function () {
    SearchService.currentItem = this.item;
    $location.url('/edit/' + this.item.id + "?panel=metadata");
  };

  $scope.itemStatus = function(isPublished){
    if(isPublished) return "Published"
    else return "Draft"
  }

  init();
}

HomeController.$inject = ['$scope',
  '$rootScope',
  '$http',
  '$location',
  'ItemService',
  'SearchService',
  'Collection',
  'Contributor',
  'ItemFormattingUtils'];

