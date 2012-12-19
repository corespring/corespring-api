function MainController($scope, $rootScope, $location, Config, LaunchConfigService ) {

  console.log("MainController");


  var init = function () {

    $scope.$watch('config', function(newValue){

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

MainController.$inject = ['$scope', '$rootScope', '$location', 'Config', 'LaunchConfigService'];