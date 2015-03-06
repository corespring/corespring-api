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

  $scope.$on('goLiveRequested', function($event, item){
    $scope.goLive(item);
  });

  $scope.goLive = function(item){
    $scope.itemToPublish = item;
    $scope.showConfirmPublishModal = true;
  };
  
  $scope.goLiveConfirmed = function(){
    $scope.showConfirmPublishModal = false;

    $scope.itemToPublish.publish(function(published){
      if(!published){
        alert('Error publishing');
      }
      $scope.itemToPublish.published = published;
      $scope.itemToPublish = null;
    }, 
    function(err){
      alert(err);
    });
  };

  $scope.goLiveCancelled = function(){
    $scope.itemToPublish = null;
    $scope.showConfirmPublishModal = false;
  };



}

RootController.$inject = ['$scope', 'ItemService'];
