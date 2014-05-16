function CreateV2Ctrl($routeParams, V2ItemService) {

  "use strict";

  if (!$routeParams.type) {
    return false;
  }

  var itemService = new V2ItemService();

  itemService.create({},
    function onItemSaved(itemData) {
      window.location.href = '/web#/edit/' + itemData.id;
    },
    function onError(e) {
      alert("Error Saving Item: " + e.data.message);
    }
  );
}

CreateCtrl.$inject = ['$routeParams', 'V2ItemService'];