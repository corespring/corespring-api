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
function ItemController(
  $scope, 
  $location, 
  $routeParams, 
  ItemService,  
  Logger, 
  ItemSessionCountService) {

  $scope.devEditorVisible = false;
  
  var normalEditor = ['/v2/player/editor/',
                      $routeParams.itemId, 
                      '/index.html',
                      '?bypass-iframe-launch-mechanism=true']
   .join('');

  var devEditor = '/v2/player/dev-editor/' + $routeParams.itemId + '/index.html';
  
  $scope.v2Editor = $scope.devEditorVisible ? devEditor : normalEditor;

  $scope.backToCollections = function(){
    $location.path("/home").search('');
  };

  $scope.itemId = $routeParams.itemId;

  $scope.version = ($scope.itemId.indexOf(':') !== -1) ? $scope.itemId.split(':')[1] : '0';

  $scope.clone = function () {
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
    $scope.$emit('goLiveRequested', $scope.item);
  };

  $scope.commit = function(){
    //Commit the draft
  };

  $scope.loadItem = function() {
    ItemService.get({id: $routeParams.itemId}, function onItemLoaded(itemData) {
      $scope.item = itemData;
      ItemSessionCountService.get({id:$routeParams.itemId}, function onCountLoaded(countObject) {
        $scope.item.sessionCount = countObject.sessionCount;
        $scope.$broadcast("dataLoaded");
      });
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

  $scope.loadItem();
}

ItemController.$inject = [
  '$scope',
  '$location',
  '$routeParams',
  'ItemService',
  'Logger',
  'ItemSessionCountService'
];
  
  root.tagger = root.tagger || {};
  root.tagger.ItemController = ItemController;
  
})(this);