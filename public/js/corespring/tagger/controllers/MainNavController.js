function MainNavController($scope, $rootScope, $location, SearchService, V2ItemService, ItemDraftService) {

  "use strict";

  $scope.$on('onSearchCountComplete', function (event, count) {
    $rootScope.resultCount = count;
  });

  $scope.goToItem = function (itemId) {
    $location.path('/edit/' + itemId);
  };

  $scope.loadMore = function (index, onLoaded) {
    SearchService.loadMore(function () {
      $rootScope.items = SearchService.itemDataCollection;
      if (onLoaded) {
        onLoaded();
      }
    });
  };

  $scope.$on('onListViewOpened', function (evt) {
    $scope.editViewOpen = false;
  });

  $scope.$on('onEditViewOpened', function (evt) {
    $scope.editViewOpen = true;
  });

  $scope.$on('createNewItem', function (evt) {
    $rootScope.items = [];
  });

  $scope.createItem = function(){
     V2ItemService.create({}, function onCreated(itemData) {
      ItemDraftService.createUserDraft(itemData.id, function(draft){
        window.location.href = '/web#/edit/draft/' + draft.id;
      });
    }, function onError(e) {
        alert("Error Saving Item: " + e.data.message);
      }
    );
  };
}

MainNavController.$inject = [
  '$scope', 
  '$rootScope', 
  '$location', 
  'SearchService', 
  'V2ItemService', 
  'ItemDraftService'];
