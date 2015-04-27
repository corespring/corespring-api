function RootController($scope, $rootScope, ItemService, V2SearchService, CollectionManager) {
  "use strict";
  $scope.uiState = {
    showCollectionsPane: false
  };

  $rootScope.$watch('modals', function(n,o){
    if(n && !o){
      $scope.modals = n;
    }
  });

  $scope.$on("error", function(event, errorSubType, data){
    $scope.showErrorBox = true;
    $scope.errorSubType = errorSubType;
    var details = data ? data.error || data.message : null;
    $scope.errorDetails = details;
    $scope.errorUid = (data && data.uid) ? data.uid : null;
  });

  $scope.errorAcknowledged = function(){
    $scope.showErrorBox = false;
    $scope.errorSubType = null;
    $scope.errorDetails = null;
    $scope.errorUid = null;
  };

  $scope.sortBy = function(field) {
    if ($rootScope.searchParams.sort && $rootScope.searchParams.sort[field]) {
      $rootScope.searchParams.sort[field] *= -1;
    } else {
      $rootScope.searchParams.sort = {};
      $rootScope.searchParams.sort[field] = 1;
    }
    $scope.$broadcast("sortingOnField", field, $rootScope.searchParams.sort[field] == 1);
    $scope.search();
  };

  $scope.lazySearch = _.debounce(function() {
    $scope.search();
    $scope.$apply();
  }, 500);

  $scope.search = function() {
    var isOtherSelected = $rootScope.searchParams && _.find($rootScope.searchParams.itemType, function(e) {
      return e.label == "Other";
    });

    if (isOtherSelected) {
      $rootScope.searchParams.notSelectedItemTypes = [];
      _.each($scope.flatItemTypeDataProvided, function(e) {
        var isSelected = _.find($rootScope.searchParams.itemType, function(f) {
          return e.label == f.label;
        });
        if (!isSelected)
          $rootScope.searchParams.notSelectedItemTypes.push(e);
      });
    }
    V2SearchService.search($rootScope.searchParams, function(res) {
      $rootScope.items = applyPermissions(res);
      setTimeout(function() {
        MathJax.Hub.Queue(["Typeset", MathJax.Hub]);
      }, 200);
    });
  };

  $scope.loadMore = function() {
    V2SearchService.loadMore(function() {
      // re-bind the scope collection to the services model after result comes back
      $rootScope.items = applyPermissions(V2SearchService.itemDataCollection);
      //Trigger MathJax
      setTimeout(function() {
        MathJax.Hub.Queue(["Typeset", MathJax.Hub]);
      }, 200);

    });
  };

  function applyPermissions(items) {
    var readOnlyCollections = _.filter(CollectionManager.rawCollections, function(c) {
      return c.permission == "read";
    });
    return _.map(items, function(item) {
      var readOnlyColl = _.find(readOnlyCollections, function(coll) {
        return coll.id == item.collectionId; //1 represents read-only access
      });
      if (readOnlyColl) {
        item.readOnly = true;
      } else {
        item.readOnly = false;
      }
      return item;
    });
  }
}

RootController.$inject = ['$scope', '$rootScope', 'ItemService', 'V2SearchService','CollectionManager'];
