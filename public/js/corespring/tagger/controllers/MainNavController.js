function MainNavController($scope, $rootScope, $location, SearchService) {

  "use strict";

  $scope.$on('onSearchCountComplete', function (event, count) {
    $rootScope.resultCount = count;
  });

  $scope.goToItem = function (itemId) {
    $location.path('/edit/' + itemId);
  };

  $scope.loadMore = function (index, onLoaded) {
    SearchService.loadMore(function () {
      $rootScope.items = SearchService.itemDataCollection;
      if (onLoaded) {
        onLoaded();
      }
    });
  };

  $scope.$on('onListViewOpened', function (evt) {
    console.log('received onListViewOpened');
    $scope.editViewOpen = false;
  });

  $scope.$on('onEditViewOpened', function (evt) {
    console.log('received onEditViewOpened');
    $scope.editViewOpen = true;
  });

  $scope.$on('createNewItem', function (evt) {
    $rootScope.items = [];
  });
}

MainNavController.$inject = ['$scope', '$rootScope', '$location', 'SearchService'];
