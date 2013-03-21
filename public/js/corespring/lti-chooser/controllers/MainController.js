function MainController($scope,  $location ) {

  var init = function () {

    $scope.$watch('config', function(newConfig,oldConfig){
      if ($scope.hasItemId()) {
        $location.url("/view/" + $scope.getItemId());
      }
      else {
        $location.url("/browse");
      }
    });
  };
  init();
}

MainController.$inject = ['$scope',  '$location'];