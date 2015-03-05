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
function ItemController($scope, $location, $routeParams, V2ItemService, $rootScope, Collection, ServiceLookup, $http, ItemMetadata, Logger, ItemSessionCountService) {

  var itemService = new V2ItemService();

  $scope.v2Editor = "/v2/player/editor/" + $routeParams.itemId + "/index.html";

  $scope.backToCollections = function(){
    $location.path("/home").search('');
  };

  $scope.itemId = $routeParams.itemId;

  $scope.version = ($scope.itemId.indexOf(':') !== -1) ? $scope.itemId.split(':')[1] : '0';

  $scope.clone = function () {
    $scope.showProgressModal = true;
    itemService.clone({id: $routeParams.itemId}, function onCloneSuccess(data) {
      $scope.showProgressModal = false;
      $location.path('/edit/' + data.id);
    }, function onError(error) {
      $scope.showProgressModal = false;
      alert("Error cloning item: " + JSON.stringify(error));
    });
  };
}

ItemController.$inject = [
  '$scope',
  '$location',
  '$routeParams',
  'V2ItemService',
  '$rootScope',
  'Collection',
  'ServiceLookup',
  '$http',
  'ItemMetadata',
  'Logger',
  'ItemSessionCountService'
];
  
  root.tagger = root.tagger || {};
  root.tagger.ItemController = ItemController;
  
})(this);