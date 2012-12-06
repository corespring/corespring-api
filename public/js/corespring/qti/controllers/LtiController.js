function LtiController($scope, $http, Config){

  var hasSubmitted = false;

  $scope.$on('assessmentItem_submit', function (event, itemSession, onSuccess, onError) {
    hasSubmitted = true;
  });

  $scope.$watch('itemSession.isFinished', function(newValue, oldValue){

    if(newValue && hasSubmitted){
      console.log("the session is finished - notify the LTI App");

      $http.get("/lti/assignment/" + Config.assignmentId + "/process")
        .success(function(data, status, headers, config) {
          //console.log("success...");
          document.location.href = data.returnUrl;
        }).error(function(data, status, headers, config) {
          $scope.status = status;
          //console.warn("Error occurred: " + data );
        });
    }
  });
}

LtiController.$inject = ['$scope', '$http', 'Config'];