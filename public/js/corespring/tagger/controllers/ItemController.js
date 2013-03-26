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
function ItemController($scope, $location, $routeParams, ItemService, $rootScope, Collection, ServiceLookup, $http, $timeout) {

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
    // Trigger iframe reload
    var oldvalue = $scope.corespringApiUrl;
    $scope.corespringApiUrl = "";
    $timeout(function () {
      $scope.corespringApiUrl = oldvalue;
    });
  };

  $scope.togglePreview = function () {
    $scope.previewVisible = !$scope.previewVisible;
    $scope.$broadcast("panelOpen");
  };

  $scope.$watch("previewVisible", function (newValue) {
    $scope.previewClassName = newValue ? "preview-open" : "preview-closed";
    $scope.corespringApiUrl = newValue ? ("/testplayer/item/" + $routeParams.itemId + "/run") : "";
    $scope.fullPreviewUrl = "/web/item-preview/" + $routeParams.itemId;
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
      return d.value == type
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
    var panel = ["metadata", "supportingMaterials", "content"].indexOf(panelName) == -1 ? "metadata" : panelName;
    $scope.currentPanel = panel;
    $scope.$broadcast("tabSelected");
    enterEditorIfInContentPanel();
    updateLocation($scope.currentPanel);
  };

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

  $scope.loadItem = function () {
    ItemService.get({id: $routeParams.itemId}, function onItemLoaded(itemData) {
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
      alert("Error cloning item: " + error.toString())
    });
  };
  //*******item versioning*********//
  $scope.increment = function(){
      $scope.showSaveWarning = false;
      $scope.showProgressModal = true;
      $scope.itemData.increment({id:$scope.itemData.id}, function onIncrementSuccess(data){
          $scope.showProgressModal = false;
          $location.path('/edit/' + data.id);
      }, function onError(error) {
          $scope.showProgressModal = false;
          alert("Error incrementing item: " + error.toString())
      });
  }
  $scope.showSaveWarning=false
  $scope.itemVersion = 1
  $scope.$on("dataLoaded",function(newValue,oldValue){
      if(typeof $scope.itemData.version != "undefined") $scope.itemVersion = $scope.itemData.version.rev+1
      //get the most current item version given the root id of this item
//      $scope.itemData.currentItem({id:$scope.itemData.id}, function onCurrentItemSuccess(data){
//          //we have the revision number of the current item, now we compute all numbers up to that number to provide a list of all revisions
//          $scope.revisions = new Array();
//          for(var i = 0; i < data.version.rev; i++){
//              $scope.revisions[i] = data.version.rev - i
//          }
//      })
  })
  //*****************************//
  $scope.loadItem();

  $scope.$watch('itemData.pValue', function (newValue, oldValue) {
    $scope.pValueAsString = $scope.getPValueAsString(newValue);
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

  $scope.save = function () {

    if (!$scope.itemData) {
      return;
    }

    if (!$scope.suppressSave) {
      $scope.isSaving = true;
    }

    if ($scope.showResourceEditor) {
      $scope.$broadcast("saveSelectedFile");
      return;
    }

    $scope.validationResult = {};

    $scope.itemData.update({}, function (data) {
        $scope.isSaving = false;
        $scope.suppressSave = false;
        $scope.processValidationResults(data["$validationResult"]);
        $rootScope.itemData = data;
      },
      function onError() {
        console.log("Error saving item");
        $scope.isSaving = false;
        $scope.suppressSave = false;
      });
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
  '$timeout'
];

