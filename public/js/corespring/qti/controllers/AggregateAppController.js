function AggregateAppController($scope, $timeout, AggregateService, Config, MessageBridge) {


  $scope.onMessageReceived = function(e){

    var obj = JSON.parse(e.data);

    if(obj.message === "update"){

      $scope.originalSettings = angular.copy(obj.settings);

      if($scope.itemSession){
        $scope.itemSession.settings = obj.settings;
      } else{
        $scope.pendingSettings = obj.settings;
      }
    }
  };

  $timeout(function () {
    if (typeof(MathJax) != "undefined") {
      MathJax.Hub.Queue(["Typeset", MathJax.Hub]);
    }
  }, 500);


  $scope.init = function () {
    console.log("Hello From AggregateController");
    console.log("Getting Aggregate Result for " + Config.sessionIds)
    $scope.formSubmitted = true;

    AggregateService.aggregate({sessions: Config.sessionIds},
      function onLoad(data) {
        console.log(data);
        $scope.aggregate = data;
      }
    );
  }

  $scope.init();

}

AggregateAppController.$inject = ['$scope', '$timeout', 'AggregateService', 'Config', 'MessageBridge'];

