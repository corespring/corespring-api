/*
 * Controller for creating new item, in practice after the object is persisted
 * Control moves to the EditCtrl
 */
function CreateCtrl($routeParams, ItemService, NewItemTemplates) {

  "use strict";

  if (!$routeParams.type || !NewItemTemplates[$routeParams.type]) {
    return false;
  }

  var item = new ItemService();
  item.data = {
    /** Note:
     * The name data is not arbitrary it has a special significance as it
     * represents the Item.data resource.
     */
    name: "data",
    files: [
      {
        name: "qti.xml",
        "default": true,
        contentType: "text/xml",
        content: NewItemTemplates[$routeParams.type].xmlData
      }
    ]
  };


  item.$save({},
    function onItemSaved(itemData) {
      window.location.href = '/web#/edit/' + itemData.id;
    },
    function onError(e) {
      alert("Error Saving Item: " + e.data.message);
    }
  );

}

CreateCtrl.$inject = ['$routeParams', 'ItemService', 'NewItemTemplates'];
