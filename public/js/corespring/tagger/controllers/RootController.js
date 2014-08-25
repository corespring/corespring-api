function RootController($scope) {
  "use strict";
  $scope.uiState = {
    showCollectionsPane: false
  };

  $scope.$on("error", function(event, errorSubType, data){
    $scope.showErrorBox = true;
    $scope.errorSubType = errorSubType;
    $scope.errorDetails = (data && data.error) ? data.error : null;
    $scope.errorUid = (data && data.uid) ? data.uid : null;
  });

  $scope.errorAcknowledged = function(){
    $scope.showErrorBox = false;
    $scope.errorSubType = null;
    $scope.errorDetails = null;
    $scope.errorUid = null;
  };
}

RootController.$inject = ['$scope'];
