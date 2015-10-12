function MainNavController($scope,
                           $rootScope,
                           $location,
                           V2SearchService,
                           V2ItemService) {

  "use strict";

  $scope.navigateToRoot = function(root) {
    if (_.isFunction($scope.navigationHooks.beforeUnload)) {
      $scope.navigationHooks.beforeUnload(function() {
        window.location.href = root;
      });
    } else {
      window.location.href = root;
    }
  };

  $scope.$on('onSearchCountComplete', function(event, count) {
    $rootScope.resultCount = count;
  });

  $scope.goToItem = function(itemId) {
    $location.path('/edit/' + itemId);
  };

  $scope.loadMore = function(index, onLoaded) {
    V2SearchService.loadMore(function() {
      $rootScope.items = V2SearchService.itemDataCollection;
      if (onLoaded) {
        onLoaded();
      }
    });
  };

  $scope.$on('onListViewOpened', function(evt) {
    $scope.editViewOpen = false;
  });

  $scope.$on('onEditViewOpened', function(evt) {
    $scope.editViewOpen = true;
  });

  $scope.$on('createNewItem', function(evt) {
    $rootScope.items = [];
  });

  $scope.createItem = function() {
    V2ItemService.create({}, function onCreated(itemData) {
        $location.url('/edit/draft/' + itemData.id);
      }, function onError(e) {
        alert("Error creating item: " + e.data.message);
      }
    );
  };
}

MainNavController.$inject = [
  '$scope',
  '$rootScope',
  '$location',
  'V2SearchService',
  'V2ItemService'];
