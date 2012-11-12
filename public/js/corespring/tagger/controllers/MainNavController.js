function MainNavController($scope){
  $scope.pagerText = "";

  $scope.$on('onSearchCountComplete', function(event, count){
    $scope.pagerText = count;
  });
}

MainNavController.$inject = ['$scope'];
