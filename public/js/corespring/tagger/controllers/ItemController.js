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
 * Controller for editing ItemService
 */
function ItemController($scope, $location, $routeParams, ItemService, $rootScope, Collection, ServiceLookup, $http, AccessToken) {

    function loadStandardsSelectionData() {
        $http.get( ServiceLookup.getUrlFor('standardsTree')).success(function (data) {
            $scope.standardsOptions = data;
        });
    }


    function initPane($routeParams) {

        var panelName = 'item';
        if ($routeParams.panel) {
            panelName = $routeParams.panel;
        }
        $scope.changePanel(panelName);
        loadStandardsSelectionData();
    }

    function initPreviewVisibleFromParams($routeParams) {
        return ($routeParams && $routeParams.preview === "1");
    }

    function initFileListVisibleFromParams($routeParams) {
        return ($routeParams && $routeParams.fileList === "1")
    }

    /**
     * Update the location search settings to reflect the ui state
     * Generates: ?preview=[1|0]&panel=[content|metadata]
     * @param panelName
     * @param previewVisible
     */
    function updateLocation(panelName, previewVisible, fileListVisible) {
        var current = $location.search();
        var previewNumber = previewVisible ? "1" : "0";
        var fileListNumber = fileListVisible ? "1" : "0";

        if (current.panel == panelName
            &&
            current.preview == previewNumber
            &&
            current.fileList == fileListNumber) {
            return;
        }

        $location.search("panel=" + panelName + "&preview=" + previewNumber + "&fileList=" + fileListNumber);
    }


    var self = this;
    var itemId = $routeParams.itemId;

    $scope.$root.mode = "edit";

    //ui nav
    $scope.previewVisible = initPreviewVisibleFromParams($routeParams);
    $scope.fileListVisible = initFileListVisibleFromParams($routeParams);

    $scope.$watch("previewVisible", function (newValue) {
        $scope.previewClassName = newValue ? "preview-open" : "preview-closed";
        updateLocation($scope.currentPanel, $scope.previewVisible, $scope.fileListVisible);
    });


    $scope.$watch("fileListVisible", function (newValue) {
        $scope.fileListClassName = newValue ? "file-list-open" : "file-list-closed";
        updateLocation($scope.currentPanel, $scope.previewVisible, $scope.fileListVisible);
    });

    $scope.changePanel = function (panelName) {
        $scope.currentPanel = panelName;
        $scope.$broadcast("tabSelected");
        updateLocation($scope.currentPanel, $scope.previewVisible);
    };

    $scope.editItem = function(){
       $location.url('/edit/' + $scope.itemData.id);
    };

    $scope.$on("panelOpen", function (event, panelOpen) {
        $scope.editorClassName = panelOpen ? "preview-open" : "preview-closed";

    });

    $scope.togglePreview = function () {
        $scope.previewVisible = !$scope.previewVisible;
        $scope.$broadcast("panelOpen");
    };

    $scope.toggleFileList = function () {
        $scope.fileListVisible = !$scope.fileListVisible;
        $scope.$broadcast("panelOpen");
    };

    /**
     * file upload event handlers
     */
    $scope.$on("uploadCompleted", function (event, result) {

        $scope.$apply(function () {
            var resultObject = $.parseJSON(result);

            if (resultObject.error != null) {
                alert("Error adding the file");
                console.warn("uploadCompleted:Error: " + resultObject.error);
                return;
            }
            $scope.itemData.files.push({ filename:resultObject.fileName });
            $scope.save();
        })

    });

    $scope.$on("uploadStarted", function (event) {
        console.log("controller: uploadStarted");
    });

    $scope.$on("fileSizeGreaterThanMax", function (event, file, maxSize) {
        //TODO: use a twitter modal instead
        alert("The file : " + file.name + " is too big to upload (the max size is: " + maxSize + "kB).")
    });


    /**
     * file-uploader callback.
     * @param file - the file selected by the user
     * @return a url where this file will be uploaded
     */
    $scope.calculateUploadUrl = function (file) {
        if (file == null) {
            throw "ItemController:calculateUploadUrl - the file is null"
        }
        return $scope.getUrl("uploadFile", itemId, file.name);
    };

    $scope.calculateSupportingMaterialUploadUrl = function (file) {
        if (file == null) {
            throw "ItemController:calculateSupportingMaterialUploadUrl - the file is null"
        }
        return $scope.getUrl("uploadSupportingMaterial", itemId, file.name);
    };

    $scope.getUrl = function (action, itemId, fileName) {
        var templateUrl = ServiceLookup.getUrlFor(action);

        if (templateUrl == null) {
            throw "Can't find url for action: " + action;
        }

        return templateUrl.replace("{itemId}", itemId).replace("{fileName}", fileName)
    };

    $scope.showFile = function (file) {
        if (file.filename != null) {
            var url = $scope.getUrl("viewFile", itemId, file.filename);
            window.open(url, '_blank');
        }
    };

    $scope.removeFile = function (file) {

        if ($scope.itemData.files == null) {
            throw "Can't remove null item from array";
        }
        var result = $scope.itemData.files.removeItem(file);

        if (result == file) {

            var deleteUrl = $scope.getUrl("deleteFile", itemId, file.filename);

            $.get(deleteUrl, function (result) {

                var resultObject = null;

                if (typeof(result) == "string") {
                    resultObject = $.parseJSON(result);
                }
                else {
                    resultObject = result;
                }

                if (!resultObject.success) {
                    console.warn("Couldn't delete the following resource: " + resultObject.key);
                }
            });

            $scope.save();
        } else {
            throw "Couldn't remove item from files: " + file.filename;
        }
    };

    $scope.destroyConfirmed = function () {
        $scope.showConfirmDestroyModal = false;
        $scope.itemData.destroy(function (result) {
            if (result.success) {
                $location.path('/item-collection');
            }
        });

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
            $scope.initialXmlData = itemData.xmlData;

            $scope.$watch("itemData.xmlData", function (newValue, oldValue) {
                $scope.processedXmlData = $scope.processData(newValue, itemId, $scope.itemData.files);

                if (newValue == $scope.initialXmlData) {
                    $scope.processValidationResults($scope.itemData["$validationResult"]);
                } else {
                    $scope.showExceptions = false;
                    $scope.validationResult = ( $scope.validationResult || {} );
                    $scope.validationResult.exceptions = [];
                }
            });

            if ($scope.itemData.collection) {
                $scope.selectedCollection = $scope.itemData.collection.name;
            }

            $scope.$broadcast("dataLoaded");
        });
    };

    $scope.accessToken = AccessToken;

    if (!AccessToken.token) {

        $scope.$watch('accessToken.token', function(newValue){
            if(newValue){
                $scope.collections = Collection.query({access_token: $scope.accessToken.token});
                $scope.loadItem();
            }
        })
    } else {

        $scope.collections = Collection.query({access_token: $scope.accessToken.token});
        $scope.loadItem();
    }


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

    $scope.isClean = function () {
        return angular.equals(self.original, $scope.itemData);
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

        $scope.itemData.update(function (data) {
            $scope.isSaving = false;
            $scope.suppressSave = false;
            $scope.processValidationResults(data["$validationResult"])
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
        if (newValue != "Other") {
            if ($scope.itemData != undefined) {
                $scope.itemData.itemTypeOther = null;
            }
        }
    });

    $scope.updateItemType = function () {
        $scope.itemData.itemType = "Other";
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

