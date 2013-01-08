function MainNavController($scope, $rootScope, $location, SearchService){

  $scope.$on('onSearchCountComplete', function(event, count){
    $rootScope.resultCount = count;
  });

  $scope.goToItem = function(itemId){
      $location.path('/edit/' + itemId);
  };

  $scope.loadMore = function(index, onLoaded){
    SearchService.loadMore(function () {
      $rootScope.items = SearchService.itemDataCollection;
      if(onLoaded){ onLoaded() }
    });
  };

  $scope.$on('onListViewOpened', function (evt) {
    console.log('received onListViewOpened');
    $scope.editViewOpen = false;
  });

  $scope.$on('onEditViewOpened', function (evt) {
    console.log('received onEditViewOpened');
    //$scope.updatePagerText();
    $scope.editViewOpen = true;
  });

  $scope.$on('createNewItem', function (evt) {
    $rootScope.items = [];
    //$scope.updatePagerText();
  });
}

MainNavController.$inject = ['$scope', '$rootScope', '$location', 'SearchService'];
