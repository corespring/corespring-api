(function(root){ 
  'use strict';

/**
 * Remove Item utility method
 */
if (Array.prototype.removeItem === null) Array.prototype.removeItem = function (item) {
  var itemIndex = this.indexOf(item);

  if (itemIndex == -1) {
    return null;
  }

  return this.splice(itemIndex, 1)[0];
};

/**
 * Controller for editing Item
 */
function EditDraftController(
  $scope, 
  $location, 
  $routeParams, 
  ItemDraftService,  
  Logger, 
  ItemSessionCountService) {

  $scope.devEditorVisible = false;
  
  var normalEditor = ['/v2/player/editor/',
                      $routeParams.draftId, 
                      '/index.html',
                      '?bypass-iframe-launch-mechanism=true']
   .join('');

  var devEditor = '/v2/player/dev-editor/' + $routeParams.draftId + '/index.html';
  
  $scope.v2Editor = $scope.devEditorVisible ? devEditor : normalEditor;

  $scope.backToCollections = function(){
    $location.path("/home").search('');
  };

  $scope.draftId = $routeParams.draftId;

  $scope.saveBackToItem = function(){
  	ItemDraftService.commit($scope.draftId, function success(){
  		Logger.info('commit successful');
  	}, function error(err){
  		Logger.warn(err);
  	});
  };

  $scope.clone = function () {

    $scope.$emit('clone-item', $scope.itemId, {
      onStart: function(){
        $scope.showProgressModal = true;
      },
      onComplete: function(err, result){
        $scope.showProgressModal = false;

        if(err){

        } else {
          
        }
      }
    });
    $scope.showProgressModal = true;

    $scope.item.clone(function onCloneSuccess(data) {
      $scope.showProgressModal = false;
      $location.path('/edit/' + data.id);
    }, function onError(error) {
      $scope.showProgressModal = false;
      alert("Error cloning item: " + JSON.stringify(error));
    });
  };

  $scope.goLive = function(){
    ItemDraftService.goLive($scope.draftId, function(result){
      Logger.info('go live complete');
      Logger.info(result);
    }, 
    function(err){
      Logger.error(err);
    });
  };

  $scope.commit = function(){
    //Commit the draft
  };


  //$scope.version = ($scope.itemId.indexOf(':') !== -1) ? $scope.itemId.split(':')[1] : '0';

  $scope.clone = function () {

    throw new Error('What does clone do when you are editing a user draft?');
    /*
    $scope.showProgressModal = true;
    $scope.item.clone(function onCloneSuccess(data) {
      $scope.showProgressModal = false;
      $location.path('/edit/' + data.id);
    }, function onError(error) {
      $scope.showProgressModal = false;
      alert("Error cloning item: " + JSON.stringify(error));
    });
    */
  };

  $scope.loadDraftItem = function() {
    ItemDraftService.get({id: $routeParams.draftId}, function onItemLoaded(draft) {
      $scope.draft = draft;
      $scope.itemId = draft.itemId;
      $scope.baseId = $scope.itemId.indexOf(':') !== -1 ? $scope.itemId.split(':')[0] : $scope.itemId;
      $scope.version = $scope.itemId.indexOf(':') !== -1 ? $scope.itemId.split(':')[1] : '';
      console.warn('ItemSessionCount doesn\'t apply for a user draft');
    });
  };

  $scope.showDevEditor = function(){
    $scope.devEditorVisible = true;
    $scope.v2Editor = devEditor;
  };

  $scope.showEditor = function(){
    $scope.devEditorVisible = false;
    $scope.v2Editor = normalEditor;
  };

  $scope.loadDraftItem();
}

EditDraftController.$inject = [
  '$scope',
  '$location',
  '$routeParams',
  'ItemDraftService',
  'Logger',
  'ItemSessionCountService'
];
  
  root.tagger = root.tagger || {};
  root.tagger.EditDraftController = EditDraftController;
  
})(this);