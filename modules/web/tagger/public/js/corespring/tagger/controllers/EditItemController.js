'use strict';

/**
 * Controller for editing Item
 */
function EditItemController($scope, $location, $routeParams, ItemService, $rootScope, MongoQueryUtils, Select2Adapter, Collection, ServiceLookup, $http, ItemMetadata, Logger, ItemSessionCountService) {

  $scope.v2Editor = "/v2/player/editor/" + $routeParams.itemId + "/index.html";
  $scope.filteredDomainValues = {};

  function loadStandardsSelectionData() {
    $http.get(ServiceLookup.getUrlFor('standardsTree')).success(function(data) {
      $scope.standardsOptions = data;
    });
  }

  function loadDomainData() {
    $http.get(ServiceLookup.getUrlFor('domain')).success(function(data) {
      $scope.domainValues = {};
      for (var key in data) {
        $scope.domainValues[key] = _.sortBy(data[key], 'name');
      }
      updateStandardDomains();
    });
  }

  function updateUnusedDomains() {
    var newValues = {};
    for (var key in $scope.domainValues) {
      newValues[key] = _.chain($scope.domainValues[key]).filter(function(domain) {
        return !hasDomain(domain);
      }).sortBy('name').value();
    }
    $scope.filteredDomainValues = newValues;
  }

  function domainByStandard(standard) {
    var domain = _.chain($scope.domainValues).values().flatten().find(function(domain) {
      return domain.standards.indexOf(standard) !== -1;
    }).value();
    return (domain && domain.name) ? domain.name : undefined;
  }

  function updateStandardDomains() {
    if ($scope.itemData && $scope.itemData.standards && $scope.domainValues) {
      $scope.itemData.standardDomains = _($scope.itemData.standards).pluck('dotNotation').map(domainByStandard).sort();
      $scope.itemData.domains = _.filter($scope.itemData.domains, function(domain) {
        return $scope.itemData.standardDomains.indexOf(domain) === -1;
      });
      updateUnusedDomains();
    }
  }

  $scope.$watch('itemData.standards', updateStandardDomains);

  function loadWritableCollections() {
    function writable(collections) {
      return _.filter(collections, function(collection) {
        return collection.permission == "write";
      });
    }

    Collection.get({}, function(data) {
        $scope.collections = writable(data);
      },
      function(err) {
        Logger.error("error when loading collections in EditItemController: " + JSON.stringify(err))
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
    loadWritableCollections();
    loadDomainData();
    $scope.$watch(
      function() {
        return $location.url();
      },
      function(path) {
        var panel = $location.search().panel;
        if (panel) {
          $scope.changePanel(panel);
        }
      });
  }

  $scope.launchV1Player = function() {
    new com.corespring.players.ItemPlayer("#item-preview-target", {
        mode: "preview",
        itemId: $scope.itemData.id,
        omitSubmitButton: false
      }
    );
  };

  $scope.launchV2Player = function() {

    var options = {
      mode: "gather",
      itemId: $scope.itemData.id,
      evaluate: $scope.modeSettings,
      width: '100%'
    };

    $scope.v2player = new org.corespring.players.ItemPlayer('#item-preview-target', options, $scope.handlePlayerError);
  };

  $scope.$watch('showV2Preview', function(newValue) {
    if (!newValue) {
      $scope.v2CatalogUrl = "";
    }
  });

  function hasDomain(domain) {
    return $scope.itemData.standardDomains.indexOf(domain.name) >= 0 ||
      $scope.itemData.domains.indexOf(domain.name) >= 0;
  }


  $scope.$watch('newDomain', $scope.addDomain);

  $scope.removeDomain = function(domain) {
    var domains = $scope.itemData.domains;
    var index = domains.indexOf(domain);

    if (index !== -1) {
      domains.splice(index, 1);
      $scope.itemData.domains = domains;
    }
    updateUnusedDomains();
  };

  $scope.addDomain = function(newDomain) {
    if (newDomain !== undefined) {
      console.log('scope.itemData', $scope.itemData);
      console.log('newDomain', newDomain);
      if (!hasDomain(newDomain)) {
        $scope.itemData.domains.push(newDomain.name);
        $scope.itemData.domains = $scope.itemData.domains.sort();
      }
      $scope.newDomain = undefined;
      updateUnusedDomains();
    }
  };


  $scope.launchV2Preview = function() {
    $scope.v2CatalogUrl = '/v2/player/catalog/' + $scope.itemData.id + '/index.html'
    $scope.showV2Preview = true;
  };

  function isV2() {
    return $scope.versionOverride ? $scope.versionOverride === 2 : $('.preview').data('version') === 2;
  }

  $scope.isV1 = function() {
    return !isV2();
  };

  $scope.isV2 = function() {
    return isV2();
  };

  $scope.devUrl = function() {
    return ($scope.itemData && $scope.itemData.id) ?
    '/v2/player/dev-editor/' + $scope.itemData.id + '/index.html' : undefined;
  };

  $scope.changePlayerVersion = function() {
    $scope.versionOverride = $scope.versionOverride === 1 ? 2 : 1;
    if (isV2()) {
      $scope.launchV2Player();
    } else {
      $scope.launchV1Player();
    }
  };

  $scope.otherPlayerVersion = function() {
    return isV2() ? 1 : 2;
  };

  $scope.togglePreview = function() {
    $scope.previewVisible = !$scope.previewVisible;
    $scope.$broadcast("panelOpen");
    if (isV2()) {
      $scope.launchV2Player();
    } else {
      $scope.launchV1Player();
    }
  };

  $scope.$watch("previewVisible", function(newValue) {
    $scope.previewClassName = newValue ? "preview-open" : "preview-closed";
    $scope.corespringApiUrl = newValue ? ("/player/item/" + $routeParams.itemId + "/preview") : "";
    $scope.fullPreviewUrl = "/player/item/" + $routeParams.itemId + "/profile";
  });

  $scope.deleteItem = function(item) {
    $scope.itemToDelete = item;
    $scope.showConfirmDestroyModal = true;
  };

  $scope.deleteConfirmed = function() {
    ItemService.remove({id: $scope.itemToDelete.id},
      function(result) {
        $scope.itemToDelete = null;
        $location.path("/web");
      }
    );
    $scope.itemToDelete = null;
    $scope.showConfirmDestroyModal = false;
  };

  $scope.deleteCancelled = function() {
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
    var foundType = _.find($scope.itemData.$itemTypeDataProvider, function(d) {
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
      var substitutions = {itemId: $routeParams.itemId};
      urls.uploadFile = ServiceLookup.getUrlFor('uploadDataFile', substitutions);
      urls.createFile = ServiceLookup.getUrlFor('createDataFile', substitutions);
      urls.updateFile = ServiceLookup.getUrlFor('updateDataFile', substitutions);
      urls.deleteFile = ServiceLookup.getUrlFor('deleteDataFile', substitutions);
      $rootScope.$broadcast('enterEditor', $scope.itemData.data, false, urls, ["qti.xml"],
        $scope.itemData.id, $scope.itemData.latest);
    }
    else {
      $rootScope.$broadcast('leaveEditor');
    }
  }

  //event handlers for the enter/leave edit events.
  $scope.$on('enterEditor', function() {
    $scope.showResourceEditor = true;
  });

  $scope.$on('leaveEditor', function() {
    $scope.showResourceEditor = false;
  });

  var isViewingMetadataPanel = function() {
    return $scope.currentPanel == "orgMetadata";
  };

  $scope.changePanel = function(panelName) {
    var panel = ["metadata", "supportingMaterials", "content", "orgMetadata"].indexOf(panelName) == -1 ? "metadata" : panelName;
    $scope.currentPanel = panel;
    $scope.$broadcast("tabSelected");
    enterEditorIfInContentPanel();
    updateLocation($scope.currentPanel);
  };

  $scope.changeToOrgMetadata = function(mdKey) {
    $scope.changePanel("orgMetadata");
    $scope.selectedMetadata = mdKey;
  };

  $scope.editItem = function() {
    $location.url('/edit/' + $scope.itemData.id);
  };

  /**
   * optional callback from strap-tabs
   */
  $scope.onTabSelect = function(tab) {
    $rootScope.$broadcast("tabSelected");
    $scope.suppressSave = true;
    $scope.save();
  };

  $scope.processData = function(rawData, itemId, itemFiles) {

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

  $scope.loadItemMetadata = function() {
    ItemMetadata.get({id: $routeParams.itemId}, function onItemMetadataLoaded(itemMetadata) {
      $scope.itemMetadata = itemMetadata;

      if (isViewingMetadataPanel()) {
        $scope.selectedMetadata = $scope.itemMetadata[0].metadataKey;
      }
    });
  };


  $scope.loadItem = function() {
    ItemService.get({id: $routeParams.itemId}, function onItemLoaded(itemData) {
      $rootScope.itemData = itemData;
      ItemSessionCountService.get({id: $routeParams.itemId}, function onItemLoaded(countObject) {
        $rootScope.itemData.sessionCount = countObject.sessionCount;
        $scope.$broadcast("dataLoaded");
      });

      enterEditorIfInContentPanel();
      initItemType();

    });
  };

  $scope.clone = function() {
    $scope.showProgressModal = true;
    $scope.itemData.clone(function onCloneSuccess(data) {
      $scope.showProgressModal = false;
      $location.path('/edit/' + data.id);
    }, function onError(error) {
      $scope.showProgressModal = false;
      alert("Error cloning item: " + JSON.stringify(error))
    });
  };

  $scope.showSaveWarning = false;

  $scope.itemVersion = 1;
  $scope.$on("dataLoaded", function(newValue, oldValue) {
    $scope.itemVersion = parseInt($scope.itemData.id.split(":")[1]) + 1;
    $scope.isPublished = $scope.itemData.published;
  });

  $scope.loadItem();
  $scope.loadItemMetadata();

  $rootScope.$on('showSaveWarning', function() {
    $scope.$apply('showSaveWarning=true');
  });

  $scope.$watch('itemData.pValue', function(newValue, oldValue) {
    $scope.pValueAsString = $scope.getPValueAsString(newValue);
  });

  $scope.$watch('isPublished', function() {
    if ($scope.isPublished) {
      $scope.itemStatus = "published";
      if ($scope.itemData.sessionCount == 1) {
        $scope.sessionCount = "(" + $scope.itemData.sessionCount + " response)";
      }
      else {
        $scope.sessionCount = "(" + $scope.itemData.sessionCount + " responses)"
      }
    } else {
      $scope.itemStatus = "Draft"
    }
  });

  $scope.getPValueAsString = function(value) {

    var vals = {
      "NO_VALUE": 0,
      "Very Hard": 20,
      "Moderately Hard": 40,
      "Moderate": 60,
      "Easy": 80,
      "Very Easy": 100
    };

    var getLabelFromValue = function(numberArray, valueToCheck) {
      for (var x in numberArray) {
        if (valueToCheck <= numberArray[x]) {
          return x == "NO_VALUE" ? "" : x;
        }
      }
    };
    return getLabelFromValue(vals, value);
  };

  $scope.processValidationResults = function(result) {

    if (result && result.success === false) {
      $scope.showExceptions = true;
      $scope.validationResult = {exceptions: angular.copy(result.exceptions)};
    }
    else {
      $scope.showExceptions = false;
    }
  };

  $scope.saveSelectedFileFinished = function(error) {
    $scope.isSaving = false;
    $scope.suppressSave = false;
    if (error) {
      $scope.showSaveWarning = true;
    } else {
      $scope.reloadPlayer();
    }
  };

  $scope.backToCollections = function() {
    $location.path("/home").search('');
  };

  $scope.publish = function() {
    $scope.itemData.published = true;
    $scope.itemData.update({}, function(data) {
      if (data.published) {
        $scope.isPublished = true
      } else alert("error publishing: status ok but no published property found")
    }, function(error) {
      alert("error publishing: " + JSON.stringify(error))
    })
  }

  $scope.save = function(saveItemData) {

    if (!$scope.itemData) {
      return;
    }

    if (!$scope.suppressSave) {
      $scope.isSaving = true;
    }
    if ($scope.showSaveWarning) {
      $scope.showSaveWarning = false;
    }

    if ($scope.showResourceEditor && !saveItemData) {
      $scope.$broadcast("saveSelectedFile");
      return;
    }

    if (!saveItemData && $scope.itemData.published && ($scope.itemData.sessionCount > 0)) {
      $scope.showSaveWarning = true;
      $scope.isSaving = false
    } else {
      $scope.validationResult = {};
      $scope.itemData.update({}, function(data) {
          $scope.isSaving = false;
          $scope.suppressSave = false;
          $scope.processValidationResults(data["$validationResult"]);
          if (data.id != $scope.itemData.id) {
            $location.path('/edit/' + data.id);
          } else {
            $rootScope.itemData = data;
            $scope.$broadcast("dataLoaded")
          }
          $scope.reloadPlayer();
        },
        function onError(err) {
          $scope.isSaving = false;
          $scope.suppressSave = false;
        }
      );
    }
  };

  $scope.reloadPlayer = function() {
    if (isV2()) {
      $scope.launchV2Player();
    } else {
      $scope.launchV1Player();
    }
  };

  var subjectFormatResult = function(subject) {
    var separator = " ";
    if (subject.subject) separator = ": ";
    var markup = "<blockquote>"
    markup += '<p>' + subject.category + separator + '</p>';
    markup += '<p>' + subject.subject + '</p>';
    markup += '</blockquote>';
    return markup;
  };

  var subjectFormatSelection = function(subject) {
    var separator = " ";
    if (subject.subject) separator = ": ";
    return subject.category + separator + subject.subject;
  };


  var addFieldIfApplicable = function(item, filter, key) {
    if (!item) {
      return;
    }
    if (item.name && item.name.toLowerCase() == "all") {
      return;
    }
    filter[key] = item.name;
  };

  $scope.createStandardMongoQuery = function(searchText, fields) {

    if ($scope.standardAdapter.subjectOption == "all") {
      return createMongoQuery(searchText, ['subject'].concat(fields));
    } else {
      var query = MongoQueryUtils.fuzzyTextQuery(searchText, fields);
      addFieldIfApplicable($scope.standardAdapter.subjectOption, query, "subject");
      addFieldIfApplicable($scope.standardAdapter.categoryOption, query, "category");

      if ($scope.standardAdapter.subCategoryOption && $scope.standardAdapter.subCategoryOption != "All") {
        query["subCategory"] = $scope.standardAdapter.subCategoryOption;
      }
      return JSON.stringify(query);
    }
  };

  $scope.standardAdapter = new Select2Adapter(
    ServiceLookup.getUrlFor('standards'),
    "Choose a standard",
    $scope.createStandardMongoQuery,
    ['dotNotation', 'category', 'subCategory', 'standard']
  );

  $scope.standardAdapter.valueSetter = function(newItem) {
    if ($scope.itemData.standards != null) {
      $scope.itemData.standards.push(newItem);
    }
  };

  $scope.standardAdapter.formatSelection = function(standard) {

    setTimeout(function() {
      $(".standard-adapter-result").tooltip();
    }, 500);
    return "<span class='standard-adapter-result' data-title='" + standard.standard + "'>" + standard.dotNotation + "</span>";
  };

  $scope.standardAdapter.formatResult = function(standard) {
    var markup = "<blockquote>";
    markup += '<p>' + standard.standard + '</p>';
    markup += '<small>' + standard.dotNotation + ', ' + standard.subject + ', ' + standard.subCategory + '</small>';
    markup += '<small>' + standard.category + '</small>';
    markup += '</blockquote>';
    return markup;
  };

  $scope.standardAdapter.tags = true;

  $scope.selectPrimarySubject = new Select2Adapter(
    ServiceLookup.getUrlFor('subject'),
    {subject: "choose a subject", category: "Subject", id: ""},
    createMongoQuery,
    ['subject', 'category']
  );
  $scope.selectPrimarySubject.formatResult = subjectFormatResult;
  $scope.selectPrimarySubject.formatSelection = subjectFormatSelection;

  $scope.selectRelatedSubject = new Select2Adapter(
    ServiceLookup.getUrlFor('subject'),
    {subject: "choose a subject", category: "Subject", id: ""},
    createMongoQuery,
    ['subject', 'category']
  );
  $scope.selectRelatedSubject.formatResult = subjectFormatResult;
  $scope.selectRelatedSubject.formatSelection = subjectFormatSelection;

  $scope.$watch("itemData.itemType", function(newValue) {
    if (newValue != $scope.otherItemType) {
      $scope.otherItemType = "";
    }
  });

  $scope.updateItemType = function() {
    $scope.itemData.itemType = $scope.otherItemType;
  };

  $scope.getKeySkillsSummary = function(keySkills) {
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
   * @param {string} searchText - the text to query for
   * @param {array} fields - an array of the fields to find the searchtext in
   */
  function createMongoQuery(searchText, fields) {
    return JSON.stringify(MongoQueryUtils.fuzzyTextQuery(searchText, fields));
  }


  //initialisation:
  initPane($routeParams);


  // end EditCrtl
}

EditItemController.$inject = [
  '$scope',
  '$location',
  '$routeParams',
  'ItemService',
  '$rootScope',
  'MongoQueryUtils',
  'Select2Adapter',
  'Collection',
  'ServiceLookup',
  '$http',
  'ItemMetadata',
  'Logger',
  'ItemSessionCountService'
];
