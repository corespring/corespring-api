function LtiController($scope, $http, Config) {

  "use strict";

  var hasSubmitted = false;

  $scope.$on('assessmentItem_submit', function () {
    hasSubmitted = true;
  });

  $scope.$watch('itemSession.isFinished', function (newValue) {

    if (newValue && hasSubmitted) {

      $http.get("/lti/assignment/" + Config.assessmentId + "/" + Config.resultSourcedId + "/process")
        .success(function (data) {
          document.location.href = data.returnUrl;
        }).error(function (data, status) {
          $scope.status = status;
        });
    }
  });
}

LtiController.$inject = ['$scope', '$http', 'Config'];