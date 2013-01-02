function MainController($scope,  $location ) {

  var init = function () {

    $scope.$watch('config', function(){

      if ($scope.config.itemId) {
        $location.url("/view/" + $scope.config.itemId);
      }
      else {
        $location.url("/browse");
      }
    });
  };
  init();
}

MainController.$inject = ['$scope',  '$location'];