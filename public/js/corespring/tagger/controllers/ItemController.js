'use strict';

/**
 * Remove Item utility method
 */
if (Array.prototype.removeItem == null) Array.prototype.removeItem = function (item) {
  var itemIndex = this.indexOf(item);

  if (itemIndex == -1) {
    return null;
  }

  return this.splice(itemIndex, 1)[0];
};

/**
 * Controller for editing Item
 */
function ItemController($scope, $location, $routeParams, ItemService, $rootScope, Collection, ServiceLookup, $http, MetadataSet) {

  function loadStandardsSelectionData() {
    $http.get(ServiceLookup.getUrlFor('standardsTree')).success(function (data) {
      $scope.standardsOptions = data;
    });
  }

  function loadCollections() {
    Collection.get({}, function (data) {
        $scope.collections = data;
      },
      function () {
        console.log("load collections: error: " + arguments);
      });
  }

  function initPane($routeParams) {
    $scope.$root.mode = 'edit';
    var panelName = 'metadata';
    if ($routeParams.panel) {
      panelName = $routeParams.panel;
    }
    $scope.changePanel(panelName);
    loadStandardsSelectionData();
    loadCollections();
    $scope.$watch(
      function () {
        return $location.url();
      },
      function (path) {
        var panel = $location.search().panel;
        if (panel) {
          $scope.changePanel(panel);
        }
      });
  }

  $scope.refreshPreview = function () {
    com.corespring.players.ItemPlayer("#item-preview-target", {
        mode : "preview",
        itemId : $scope.itemData.id,
        height: "100%"}
    );
  };

  $scope.togglePreview = function () {
    $scope.previewVisible = !$scope.previewVisible;
    $scope.$broadcast("panelOpen");

    com.corespring.players.ItemPlayer("#item-preview-target", {
      mode : "preview",
      itemId : $scope.itemData.id,
      height: "100%"}
    );
  };

  $scope.$watch("previewVisible", function (newValue) {
    $scope.previewClassName = newValue ? "preview-open" : "preview-closed";
    $scope.corespringApiUrl = newValue ? ("/player/item/" + $routeParams.itemId + "/preview") : "";
    $scope.fullPreviewUrl = "/player/item/" + $routeParams.itemId + "/profile";
  });

  $scope.deleteItem = function (item) {
    $scope.itemToDelete = item;
    $scope.showConfirmDestroyModal = true;
  };

  $scope.deleteConfirmed = function () {
    ItemService.remove({id: $scope.itemToDelete.id},
      function (result) {
        $scope.itemToDelete = null;
        $location.path("/web");
      }
    );
    $scope.itemToDelete = null;
    $scope.showConfirmDestroyModal = false;
  };

  $scope.deleteCancelled = function () {
    $scope.itemToDelete = null;
    $scope.showConfirmDestroyModal = false;
  };

  /**
   * Update the location search settings to reflect the ui state
   * Generates: ?panel=[content|metadata]
   * @param panelName
   */
  function updateLocation(panelName) {
    var current = $location.search();

    if (current.panel == panelName) {
      return;
    }
    $location.search("panel=" + panelName);
  }


  $scope.$root.mode = "edit";

  /**
   * If the itemData.itemType is not one of the defaults,
   * set otherItemType to be its value so the ui picks it up.
   */
  function initItemType() {

    var type = $scope.itemData.itemType;
    if (!type) {
      return;
    }
    var foundType = _.find($scope.itemData.$itemTypeDataProvider, function (d) {
      return _.find(d.value, function(e) {
        return e == type;
      });
    });

    if (!foundType) {
      $scope.otherItemType = type;
    }
  }

  function enterEditorIfInContentPanel() {

    if ($scope.currentPanel == 'content' && $scope.itemData) {
      var urls = {};
      var substitutions = { itemId: $routeParams.itemId };
      urls.uploadFile = ServiceLookup.getUrlFor('uploadDataFile', substitutions);
      urls.createFile = ServiceLookup.getUrlFor('createDataFile', substitutions);
      urls.updateFile = ServiceLookup.getUrlFor('updateDataFile', substitutions);
      urls.deleteFile = ServiceLookup.getUrlFor('deleteDataFile', substitutions);
      $rootScope.$broadcast('enterEditor', $scope.itemData.data, false, urls, ["qti.xml"]);
    }
    else {
      $rootScope.$broadcast('leaveEditor');
    }
  }

  //event handlers for the enter/leave edit events.
  $scope.$on('enterEditor', function () {
    $scope.showResourceEditor = true
  });

  $scope.$on('leaveEditor', function () {
    $scope.showResourceEditor = false
  });

  $scope.changePanel = function (panelName) {
    var panel = ["metadata", "supportingMaterials", "content", "orgMetadata"].indexOf(panelName) == -1 ? "metadata" : panelName;
    $scope.currentPanel = panel;
    $scope.$broadcast("tabSelected");
    enterEditorIfInContentPanel();
    updateLocation($scope.currentPanel);
  };

  $scope.changeToOrgMetadata = function (mdKey) {
    $scope.changePanel("orgMetadata");
    $scope.selectedMetadataSet = mdKey;
  }

  $scope.editItem = function () {
    $location.url('/edit/' + $scope.itemData.id);
  };

  /**
   * optional callback from strap-tabs
   */
  $scope.onTabSelect = function (tab) {
    $rootScope.$broadcast("tabSelected");
    $scope.suppressSave = true;
    $scope.save();
  };

  $scope.processData = function (rawData, itemId, itemFiles) {

    if (!itemFiles) {
      return;
    }
    var out = rawData;
    for (var i = 0; i < itemFiles.length; i++) {
      var file = itemFiles[i];
      var token = new RegExp(file.filename, "g");
      var realFileUrl = $scope.getUrl("viewFile", itemId, file.filename);
      out = out.replace(token, realFileUrl);
    }
    return out;
  };

  // broadcast an event when the Edit view is called
  $rootScope.$broadcast('onEditViewOpened');


  $scope.loadMetadataSets = function(){

    MetadataSet.get({}, function onSetsLoaded(sets){

      console.log("Loaded metadata sets", sets);

      $scope.metadataSets = sets;


      $scope.loadExtendedData()
    });
  };

  $scope.loadExtendedData = function(){

    if(!$scope.metadataSets){
      return;
    }

    ExtendedData.get({id: $scope.itemData.id, sets : })
  };


  $scope.loadItem = function () {
    ItemService.get({id: $routeParams.itemId}, function onItemLoaded(itemData) {
      console.log("ItemData arrived");
      console.log(itemData);

      // TODO: Mocking this for the time being. Format is key: label
      /*itemData.metadataSets = {
        "newclassroom": {
          label: "New Classroom",
          editorUrl: "http://localhost:5000",
          lockFields: true,
          data: {
              "Skill Number": "043",
              "Family": "4",
              "Master Question": "C"
          }
        }
      };*/

      $rootScope.itemData = itemData;
      enterEditorIfInContentPanel();
      initItemType();
      $scope.$broadcast("dataLoaded");
    });
  };

  $scope.clone = function () {
    $scope.showProgressModal = true;
    $scope.itemData.clone({id: $scope.itemData.id}, function onCloneSuccess(data) {
      $scope.showProgressModal = false;
      $location.path('/edit/' + data.id);
    }, function onError(error) {
      $scope.showProgressModal = false;
      alert("Error cloning item: " + JSON.stringify(error))
    });
  };

  $scope.showSaveWarning = false;

  $scope.itemVersion = 1;
  $scope.$on("dataLoaded", function (newValue, oldValue) {
    $scope.itemVersion = parseInt($scope.itemData.id.split(":")[1]) + 1;
    $scope.isPublished = $scope.itemData.published;
  });

  $scope.loadItem();
  $scope.loadMetadataSets();

  $rootScope.$on('showSaveWarning', function () {
    $scope.$apply('showSaveWarning=true');
  });

  $scope.$watch('itemData.pValue', function (newValue, oldValue) {
    $scope.pValueAsString = $scope.getPValueAsString(newValue);
  });

  $scope.$watch('isPublished', function(){
    console.log("isPublished was changed")
      if($scope.isPublished) {
        $scope.itemStatus = "published"
        if($scope.itemData.sessionCount == 1) $scope.sessionCount = "("+$scope.itemData.sessionCount+" response)"
        else $scope.sessionCount = "("+$scope.itemData.sessionCount+" responses)"
      } else $scope.itemStatus = "Draft"
  });

  $scope.getPValueAsString = function (value) {

    var vals = {
      "NO_VALUE": 0,
      "Very Hard": 20,
      "Moderately Hard": 40,
      "Moderate": 60,
      "Easy": 80,
      "Very Easy": 100 };

    var getLabelFromValue = function (numberArray, valueToCheck) {
      for (var x in numberArray) {
        if (valueToCheck <= numberArray[x]) {
          return x == "NO_VALUE" ? "" : x;
        }
      }
    };
    return getLabelFromValue(vals, value);
  };

  $scope.processValidationResults = function (result) {

    if (result && result.success === false) {
      $scope.showExceptions = true;
      $scope.validationResult = { exceptions: angular.copy(result.exceptions) };
    }
    else {
      $scope.showExceptions = false;
    }
  };

  $scope.saveSelectedFileFinished = function (error) {
    $scope.isSaving = false;
    $scope.suppressSave = false;
    if(error) $scope.showSaveWarning = true;
  };

  $scope.publish = function(){
    $scope.itemData.published = true;
    $scope.itemData.update({},function(data){
        if(data.published) {
            $scope.isPublished = true
        }else alert("error publishing: status ok but no published property found")
    },function(error){
        alert("error publishing: "+JSON.stringify(error))
    })
  }

  $scope.save = function (saveItemData) {

    if (!$scope.itemData) {
      return;
    }

    if (!$scope.suppressSave) {
      $scope.isSaving = true;
    }
    if($scope.showSaveWarning){
        $scope.showSaveWarning = false;
    }

    if ($scope.showResourceEditor && !saveItemData) {
      $scope.$broadcast("saveSelectedFile");
      return;
    }

    if(!saveItemData && $scope.itemData.published && ($scope.itemData.sessionCount > 0)){
        $scope.showSaveWarning = true;
        $scope.isSaving = false
    }else{
        $scope.validationResult = {};
        $scope.itemData.update({}, function (data) {
            $scope.isSaving = false;
            $scope.suppressSave = false;
            $scope.processValidationResults(data["$validationResult"]);
            if(data.id != $scope.itemData.id){
                $location.path('/edit/' + data.id);
            }else{
                $rootScope.itemData = data;
                $scope.$broadcast("dataLoaded")
            }
          },
          function onError() {
            console.log("Error saving item");
            $scope.isSaving = false;
            $scope.suppressSave = false;
          }
        );
    }
  };


  var subjectFormatResult = function (subject) {
    var separator = " ";
    if (subject.subject) separator = ": ";
    var markup = "<blockquote>"
    markup += '<p>' + subject.category + separator + '</p>';
    markup += '<p>' + subject.subject + '</p>';
    markup += '</blockquote>';
    return markup;
  };

  var subjectFormatSelection = function (subject) {
    var separator = " ";
    if (subject.subject) separator = ": ";
    return subject.category + separator + subject.subject;
  };


  var addFieldIfApplicable = function (item, filter, key) {
    if (!item) {
      return;
    }
    if (item.name && item.name.toLowerCase() == "all") {
      return;
    }
    filter[key] = item.name;
  };

  $scope.createStandardMongoQuery = function (searchText, fields) {

    if ($scope.standardAdapter.subjectOption == "all") {
      return createMongoQuery(searchText, ['subject'].concat(fields));
    } else {
      var mongoQueryMaker = new com.corespring.mongo.MongoQuery();
      var query = mongoQueryMaker.fuzzyTextQuery(searchText, fields);
      addFieldIfApplicable($scope.standardAdapter.subjectOption, query, "subject");
      addFieldIfApplicable($scope.standardAdapter.categoryOption, query, "category");

      if ($scope.standardAdapter.subCategoryOption && $scope.standardAdapter.subCategoryOption != "All") {
        query["subCategory"] = $scope.standardAdapter.subCategoryOption;
      }
      return JSON.stringify(query);
    }
  };

  $scope.standardAdapter = new com.corespring.select2.Select2Adapter(
    ServiceLookup.getUrlFor('standards'),
    "Choose a standard",
    $scope.createStandardMongoQuery,
    ['dotNotation', 'category', 'subCategory', 'standard']
  );

  $scope.standardAdapter.valueSetter = function (newItem) {
    console.log("custom value setter");
    if ($scope.itemData.standards != null) {
      $scope.itemData.standards.push(newItem);
    }
  };

  $scope.standardAdapter.formatSelection = function (standard) {

    setTimeout(function () {
      $(".standard-adapter-result").tooltip();
    }, 500);
    return "<span class='standard-adapter-result' data-title='" + standard.standard + "'>" + standard.dotNotation + "</span>";
  };

  $scope.standardAdapter.formatResult = function (standard) {
    var markup = "<blockquote>"
    markup += '<p>' + standard.standard + '</p>';
    markup += '<small>' + standard.dotNotation + ', ' + standard.subject + ', ' + standard.subCategory + '</small>';
    markup += '<small>' + standard.category + '</small>';
    markup += '</blockquote>';
    return markup;
  };

  $scope.standardAdapter.tags = true;

  $scope.selectPrimarySubject = new com.corespring.select2.Select2Adapter(
    ServiceLookup.getUrlFor('subject'),
    { subject: "choose a subject", category: "Subject", id: ""},
    createMongoQuery,
    [ 'subject', 'category' ]
  );
  $scope.selectPrimarySubject.formatResult = subjectFormatResult;
  $scope.selectPrimarySubject.formatSelection = subjectFormatSelection;

  $scope.selectRelatedSubject = new com.corespring.select2.Select2Adapter(
    ServiceLookup.getUrlFor('subject'),
    { subject: "choose a subject", category: "Subject", id: ""},
    createMongoQuery,
    [ 'subject', 'category' ]
  );
  $scope.selectRelatedSubject.formatResult = subjectFormatResult;
  $scope.selectRelatedSubject.formatSelection = subjectFormatSelection;

  $scope.$watch("itemData.itemType", function (newValue) {
    if (newValue != $scope.otherItemType) {
      $scope.otherItemType = "";
    }
  });

  $scope.updateItemType = function () {
    $scope.itemData.itemType = $scope.otherItemType;
  };

  $scope.getKeySkillsSummary = function (keySkills) {
    var count = "No";
    var skills = "Skills";

    if (keySkills) {

      if (keySkills.length > 0) {
        count = keySkills.length;
      }

      if (keySkills.length == 1) {
        skills = "Skill";
      }
    }
    return count + " Key " + skills + " selected";
  };

  /**
   * creates a mongo json query for the mongolab rest API
   * @searchText - the text to query for
   * @fields - an array of the fields to find the searchtext in
   */
  function createMongoQuery(searchText, fields) {
    return JSON.stringify(new com.corespring.mongo.MongoQuery().fuzzyTextQuery(searchText, fields));
  }


  //initialisation:
  initPane($routeParams);


  // end EditCrtl
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
  'MetadataSet'
];

