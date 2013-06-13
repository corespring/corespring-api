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

  $scope.createCollection = function(collname){
    if(collname){
      Collection.create({},{name:collname},function(data){
          if($rootScope.collections) $rootScope.collections.push(data)
      },function(err){
          console.log("create collection: error: " + err);
      })
    }
  };

  $rootScope.showCollectionWindow = false;
  $scope.openCollectionWindow = function(){
      $rootScope.showCollectionWindow = true;
  }
}

MainNavController.$inject = ['$scope', '$rootScope', '$location', 'SearchService'];
