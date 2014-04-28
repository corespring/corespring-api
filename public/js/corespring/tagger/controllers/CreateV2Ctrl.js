function CreateV2Ctrl($routeParams, ItemService, NewItemTemplates) {

  "use strict";

  if (!$routeParams.type) {
    return false;
  }

   //TODO - will need to add templates for new v2 player

   var components = {
     "3" : {
        "componentType" : "corespring-multiple-choice",
        "title": "Get a Job",
        "weight" : 4,
        "correctResponse" : { "value" : ["2"] },
        "scoreMapping" : {
          "1": 0.5,
          "2": 1,
          "3": -1,
          "4": -1
        },
        "feedback" : [
          { "value" : "1", "feedback" : "Incorrect"},
          { "value" : "2", "feedback" : "Correct", "notChosenFeedback" : "This would have been the correct one."},
          { "value" : "3", "feedback" : "Incorrect" },
          { "value" : "4", "feedback" : "Incorrect" }
        ],
        "model" : {
          "prompt": "What is 1 + 1?",
          "config": {
            "orientation": "vertical",
            "shuffle": true,
            "singleChoice": true
          },
          "choices": [
            {"label": "1", "value": "1"},
            {"label": "2", "value": "2"},
            {"label": "3", "value": "3"},
            {"label": "4", "value": "4"}
          ]
        }
       }
     }

  var item = new ItemService();
  item.playerDefinition = {
    xhtml : "<div><h2>I'm a new item</h2><corespring-multiple-choice id='3'></corespring-multiple-choice></div>",
    components: components,
    files: []
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