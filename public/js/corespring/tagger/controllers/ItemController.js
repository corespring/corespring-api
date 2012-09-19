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
function ItemController($scope, $location, $routeParams, ItemService, $rootScope, Collection, ServiceLookup, $http, AccessToken) {

    function loadStandardsSelectionData() {
        $http.get(ServiceLookup.getUrlFor('standardsTree')).success(function (data) {
            $scope.standardsOptions = data;
        });
    }

    function initPane($routeParams) {
        var panelName = 'metadata';
        if ($routeParams.panel) {
            panelName = $routeParams.panel;
        }
        $scope.changePanel(panelName);
        loadStandardsSelectionData();
    }

    /**
     * Update the location search settings to reflect the ui state
     * Generates: ?panel=[content|metadata]
     * @param panelName
     */
    function updateLocation(panelName) {
        var current = $location.search();

        if (current.panel == panelName ) {
            return;
        }
        $location.search("panel=" + panelName );
    }

    $scope.$root.mode = "edit";

    /**
     * If the itemData.itemType is not one of the defaults,
     * set otherItemType to be its value so the ui picks it up.
     */
    function initItemType(){

        var type = $scope.itemData.itemType;
        if(!type){
            return;
        }
        var foundType = _.find($scope.itemData.$itemTypeDataProvider, function(d){ return d.value == type});

        if(!foundType){
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
        $scope.currentPanel = panelName;
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

        if (itemFiles == undefined) {
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
        ItemService.get({id:$routeParams.itemId, access_token:AccessToken.token}, function onItemLoaded(itemData) {
            $scope.itemData = itemData;
            enterEditorIfInContentPanel();
            initItemType();
            if ($scope.itemData.collection) {
                $scope.selectedCollection = $scope.itemData.collection.name;
            }
            $scope.$broadcast("dataLoaded");
        });
    };

    $scope.accessToken = AccessToken;

    Collection.query({access_token:$scope.accessToken.token}, function(data){
        $scope.collections = data;
    });

    $scope.loadItem();


    $scope.$watch('itemData.pValue', function (newValue, oldValue) {
        $scope.pValueAsString = $scope.getPValueAsString(newValue);
    });

    $scope.getPValueAsString = function (value) {

        var vals = {
            "NO_VALUE":0,
            "Very Hard":20,
            "Moderately Hard":40,
            "Moderate":60,
            "Easy":80,
            "Very Easy":100 };

        var key = function (numberArray, valueToCheck) {
            for (var x in numberArray) {
                if (valueToCheck < numberArray[x]) {
                    return x == "NO_VALUE" ? "" : x;
                }
            }
        }(vals, value);

        return key;
    };

    $scope.processValidationResults = function (result) {

        if (result != null && result.success == false) {
            $scope.showExceptions = true;
            $scope.validationResult = { exceptions:angular.copy(result.exceptions) };
        }
        else {
            $scope.showExceptions = false;
        }
    };


    $scope.save = function () {
        if (!$scope.itemData) {
            return;
        }

        if (!$scope.suppressSave) {
            $scope.isSaving = true;
        }

        $scope.validationResult = {};

        $scope.itemData.update({access_token:$scope.accessToken.token}, function (data) {
            $scope.isSaving = false;
            $scope.suppressSave = false;
            $scope.processValidationResults(data["$validationResult"]);
            $scope.itemData = data;
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


    $scope.createStandardMongoQuery = function (searchText, fields) {

        if ($scope.standardAdapter.subjectOption == "all") {
            return createMongoQuery(searchText, ['subject'].concat(fields));
        } else {
            var mongoQueryMaker = new com.corespring.mongo.MongoQuery();

            var orQuery = mongoQueryMaker.fuzzyTextQuery(searchText, fields);

            var filter = [];
            if ($scope.standardAdapter.subjectOption && $scope.standardAdapter.subjectOption.name != "All") {
                filter.push({subject:$scope.standardAdapter.subjectOption.name});
            }
            if ($scope.standardAdapter.categoryOption.name != "All") {
                filter.push({category:$scope.standardAdapter.categoryOption.name});
            }
            if ($scope.standardAdapter.subCategoryOption != "All") {
                filter.push({subCategory:$scope.standardAdapter.subCategoryOption});
            }
            filter.push(orQuery);
            var query = mongoQueryMaker.and.apply(null, filter);

            return JSON.stringify(query);
        }
    };
    $scope.standardAdapter = new com.corespring.select2.Select2Adapter(
        ServiceLookup.getUrlFor('standards'),
        "4fbe6747e4b083e37574238b",
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
        "4fbe6747e4b083e37574238b",
        { subject:"choose a subject", category:"Subject", id:""},
        createMongoQuery,
        [ 'subject', 'category' ]
    );
    $scope.selectPrimarySubject.formatResult = subjectFormatResult;
    $scope.selectPrimarySubject.formatSelection = subjectFormatSelection;

    $scope.selectRelatedSubject = new com.corespring.select2.Select2Adapter(
        ServiceLookup.getUrlFor('subject'),
        "4fbe6747e4b083e37574238b",
        { subject:"choose a subject", category:"Subject", id:""},
        createMongoQuery,
        [ 'subject', 'category' ]
    );
    $scope.selectRelatedSubject.formatResult = subjectFormatResult;
    $scope.selectRelatedSubject.formatSelection = subjectFormatSelection;

    $scope.$watch("itemData.itemType", function (newValue) {
        if(newValue != $scope.otherItemType){
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
    'AccessToken'];

