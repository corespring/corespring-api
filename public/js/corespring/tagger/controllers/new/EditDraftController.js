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
  ItemSessionCountService,
  Modals) {

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
    $scope.showProgressModal = true;
    ItemDraftService.clone($scope.draftId, function (result){
      Logger.info(result);
      $scope.showProgressModal = false;
      $location.path('/edit/draft/' + result.draftId);
    }, function(err){
      Logger.error(err);
      $scope.showProgressModal = false;
    });
  };

  $scope.publish = function(){
    Modals.publish(
      function(cancelled){
        if(cancelled){
          return;
        }
        ItemDraftService.publish($scope.draftId, function(result){
          Logger.info('publish complete');
          Logger.info(result);
          $location.path('/home');
        }, 
        function(err){
          Logger.error(err);
        });
    });
  };

  $scope.loadDraftItem = function() {
    ItemDraftService.get({id: $routeParams.draftId}, 
    	function onItemLoaded(draft) {
	      $scope.draft = draft;
	      $scope.itemId = draft.itemId;
	      $scope.baseId = $scope.itemId.indexOf(':') !== -1 ? $scope.itemId.split(':')[0] : $scope.itemId;
	      $scope.version = $scope.itemId.indexOf(':') !== -1 ? $scope.itemId.split(':')[1] : '';
	      console.warn('ItemSessionCount doesn\'t apply for a user draft');
      },
      function onError(){
      	console.warn('error -> ', arguments);	
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
  'ItemSessionCountService',
  'Modals'
];
  
  root.tagger = root.tagger || {};
  root.tagger.EditDraftController = EditDraftController;
  
})(this);