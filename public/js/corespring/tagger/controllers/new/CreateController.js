(function(root) {

  /*
   * Controller for creating new item, in practice after the object is persisted
   * Control moves to the EditCtrl
   */
  function CreateCtrl($routeParams, CmsService, NewItemTemplates) {

    "use strict";

    if (!$routeParams.type || !NewItemTemplates[$routeParams.type]) {
      return false;
    }


    var item = {
      data: {
        /** Note:
         * The name data is not arbitrary it has a special significance as it
         * represents the Item.data resource.
         */
        name: "data",
        files: [{
          name: "qti.xml",
          "default": true,
          contentType: "text/xml",
          content: NewItemTemplates[$routeParams.type].xmlData
        }]
      }
    };
    
    CmsService.createFromV1Data(item, function onItemSaved(itemData) {
      window.location.href = '/web#/edit/' + itemData.id;
    }, function onError(e) {
        alert("Error Saving Item: " + e.data.message);
      }
    );
  }

  CreateCtrl.$inject = ['$routeParams', 'CmsService', 'NewItemTemplates'];

  root.tagger = root.tagger || {};
  root.tagger.CreateCtrl = CreateCtrl;

})(this);