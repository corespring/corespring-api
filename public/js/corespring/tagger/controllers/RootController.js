function RootController($scope, ItemService) {
  "use strict";
  $scope.uiState = {
    showCollectionsPane: false
  };

  $scope.$on("error", function(event, errorSubType, data){
    $scope.showErrorBox = true;
    $scope.errorSubType = errorSubType;
    var details = data ? data.error || data.message : null;
    $scope.errorDetails = details;
    $scope.errorUid = (data && data.uid) ? data.uid : null;
  });

  $scope.errorAcknowledged = function(){
    $scope.showErrorBox = false;
    $scope.errorSubType = null;
    $scope.errorDetails = null;
    $scope.errorUid = null;
  };
}

RootController.$inject = ['$scope', 'ItemService'];
