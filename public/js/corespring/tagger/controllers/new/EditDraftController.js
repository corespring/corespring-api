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
  DraftItemService,  
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

  $scope.goLive = function(){
    //1. commit the draft 1:0 -> 1:1
    //2. set published to true
    throw new Error('How does go live work when editing a user draft?');
    //$scope.$emit('goLiveRequested', $scope.item);
  };

  $scope.loadDraftItem = function() {
    DraftItemService.get({id: $routeParams.draftId}, function onItemLoaded(draft) {
      $scope.draft = draft;
      throw new Error('ItemSesisonCount doesn\'t apply for a user draft');
      /*ItemSessionCountService.get({id:$routeParams.itemId}, function onCountLoaded(countObject) {
        $scope.item.sessionCount = countObject.sessionCount;
        $scope.$broadcast("dataLoaded");
      });*/
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
  'DraftItemService',
  'Logger',
  'ItemSessionCountService'
];
  
  root.tagger = root.tagger || {};
  root.tagger.EditDraftController = EditDraftController;
  
})(this);