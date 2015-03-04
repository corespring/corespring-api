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
function ItemController($scope, $location, $routeParams, ItemService, $rootScope, Collection, ServiceLookup, $http, ItemMetadata, Logger, ItemSessionCountService) {

  $scope.v2Editor = "/v2/player/editor/" + $routeParams.itemId + "/index.html";
}

ItemController.$inject = [
  '$scope',
  '$location',
  '$routeParams',
  'ItemService',
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