function MainNavController($scope, $rootScope, $location, SearchService, V2SearchService) {

  "use strict";

  $scope.$on('onSearchCountComplete', function (event, count) {
    $rootScope.resultCount = count;
  });

  $scope.goToItem = function (itemId) {
    $location.path('/edit/' + itemId);
  };

  $scope.loadMore = function (index, onLoaded) {
    V2SearchService.loadMore(function () {
      $rootScope.items = V2SearchService.itemDataCollection;
      if (onLoaded) {
        onLoaded();
      }
    });
  };

  $scope.$on('onListViewOpened', function (evt) {
    $scope.editViewOpen = false;
  });

  $scope.$on('onEditViewOpened', function (evt) {
    $scope.editViewOpen = true;
  });

  $scope.$on('createNewItem', function (evt) {
    $rootScope.items = [];
  });

}

MainNavController.$inject = ['$scope', '$rootScope', '$location', 'SearchService', 'V2SearchService'];
