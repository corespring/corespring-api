/**
 */
function SupportingMaterialsController($scope, $rootScope, $routeParams, ItemService, ServiceLookup, AccessToken) {

    $scope.showAddResourceModal = false;
    $scope.newSmType = "upload";
    $scope.newSmName = "Rubric";
    $scope.newSmOtherName = "";
    $scope.currentFile = null;
    $scope.itemId = $routeParams.itemId;

    $scope.canCreateNewSm = function () {
        if ($scope.newSmName == "Other" && !$scope.newSmOtherName) {
            return false;
        }

        if(!$scope.itemData){
            return false;
        }

        var nameToCheck = $scope.newSmName == "Other" ? $scope.newSmOtherName : $scope.newSmName;

        var alreadyUsed = _.find($scope.itemData.supportingMaterials, function (f) {
            return f.name == nameToCheck
        });

        return alreadyUsed == null;
    };

    /**
     * Show the supporting material.
     * At the moment that means previewing the content in an iframe.
     * @param sm
     */
    $scope.showSm = function (sm) {

        function defaultFile(sm) {
            return _.find(sm.files, function (f) {
                return f['default'] == true;
            });
        }

        function isIFrameableResource(sm) {
            if (!sm.files || sm.files.length <= 0) {
                return false;
            }
            return ['pdf', 'html', 'jpg', 'png', 'doc'].indexOf(defaultFile(sm).contentType) != -1;
        }

        if (isIFrameableResource(sm)) {
            var templateUrl = ServiceLookup.getUrlFor('previewFile');
            var file = defaultFile(sm);
            var key = "";

            if (file.content) {
                key = $routeParams.itemId + "/" + sm.name + "/" + file.name;
            } else {
                key = file.storageKey;
            }
            $scope.currentHtmlUrl = templateUrl.replace("{key}", key);
            $scope.currentMaterial = sm;
        } else {
            throw "Can't preview file";
        }
    };

    $scope.createNewHtmlSm = function () {

        $scope.itemData.supportingMaterials = ($scope.itemData.supportingMaterials || []);

        var newName = $scope.newSmName == "Other" ? $scope.newSmOtherName : $scope.newSmName;

        var newHtml = {
            name:newName,
            files:[
                {
                    name:"index.html",
                    content:"<html><body>hello world</body></html>",
                    default:true,
                    contentType:"html"
                }
            ]
        };

        $scope.itemData.supportingMaterials.push(newHtml);

        $scope.showAddResourceModal = false;

        $scope.showSm(newHtml);
        //Disabled until we get teh Salat item update fix.
        //$scope.save();
    };

    $scope.calculateSmUploadUrl = function (file) {

        $scope.showAddResourceModal = false;

        if (file == null) {
            throw "ItemController:calculateSmUploadUrl - the file is null"
        }
        return $scope.getUrl("uploadSupportingMaterial", $routeParams.itemId, $scope.getSmFileName());
    };

    $scope.getSmFileName = function () {
        if ($scope.newSmName == "Other") {
            return $scope.newSmOtherName;
        }
        return $scope.newSmName;
    };

    $scope.showEditButton = function (sm) {
        function isSingleStoreFile(sm) {
            return sm.files && sm.files.length == 1 && sm.files[0].storageKey;
        }

        return !isSingleStoreFile(sm);
    };

    /**
     * Result callback from the file upload
     * @param result - the json from the server with the StoredFile properties.
     */
    $scope.onSmUploadCompleted = function (result) {
        console.log("onSmUploadCompleted!!: " + result);

        $scope.$apply(function () {
            var resultObject = $.parseJSON(result);

            if (!$scope.itemData.supportingMaterials) {
                $scope.itemData.supportingMaterials = [];
            }

            $scope.itemData.supportingMaterials.push(resultObject);
            $scope.showSm(resultObject);
            //no need to save - its already been saved by the upload
        });
    };

    $scope.editSm = function (sm) {
        $rootScope.$broadcast('enterEditor', sm, true);
    };

    $scope.removeSm = function (sm) {

        if ($scope.itemData.supportingMaterials == null) {
            throw "Can't remove from null array";
        }

        var result = $scope.itemData.supportingMaterials.removeItem(sm);

        if (result == sm) {

            var deleteUrl = $scope.getUrl("deleteSupportingMaterial", $scope.itemId, sm.name);

            function onDeleteSuccess(result) {

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
            }

            $.ajax({
                type:"DELETE",
                url:deleteUrl,
                data:{}
            }).done(onDeleteSuccess);

        } else {
            throw "Couldn't remove item from files: " + file.filename;
        }
    };

    //TODO: dup from ItemController
    $scope.getUrl = function (action, itemId, fileName) {
        var templateUrl = ServiceLookup.getUrlFor(action);

        if (templateUrl == null) {
            throw "Can't find url for action: " + action;
        }

        return templateUrl.replace("{itemId}", itemId).replace("{fileName}", fileName)
    };

    $scope.loadItem = function () {
        ItemService.get(
            {
                id:$routeParams.itemId,
                access_token:AccessToken.token
            },
            function onItemLoaded(itemData) {
                $scope.itemData = itemData;
            });
    };

    $scope.loadItem();
}

SupportingMaterialsController.$inject = [
    '$scope',
    '$rootScope',
    '$routeParams',
    'ItemService',
    'ServiceLookup',
    'AccessToken'];
