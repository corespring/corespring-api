/**
 */
function SupportingMaterialsController($scope, $rootScope, $routeParams, SupportingMaterial, ServiceLookup, AccessToken) {

    $scope.showAddResourceModal = false;
    $scope.newResourceType = "upload";
    $scope.newResourceName = "Rubric";
    $scope.newResourceOtherName = "";
    $scope.currentFile = null;
    $scope.itemId = $routeParams.itemId;

    $scope.canCreateNewResource = function () {
        if ($scope.newResourceName == "Other" && !$scope.newResourceOtherName) {
            return false;
        }

        if (!$scope) {
            return false;
        }

        var nameToCheck = $scope.newResourceName == "Other" ? $scope.newResourceOtherName : $scope.newResourceName;

        var alreadyUsed = _.find($scope.supportingMaterials, function (f) {
            return f.name == nameToCheck
        });

        return alreadyUsed == null;
    };

    /**
     * Show the supporting material.
     * At the moment that means previewing the content in an iframe.
     * @param resource
     */
    $scope.showResource = function (resource) {

        function defaultFile(resource) {
            return _.find(resource.files, function (f) {
                return f['default'] == true;
            });
        }

        var iframeables = ['pdf', 'text/html', 'image/jpg', 'image/png', 'image/jpeg', 'doc'];

        function isIFrameableResource(resource) {
            if (!resource.files || resource.files.length <= 0) {
                return false;
            }
            return iframeables.indexOf(defaultFile(resource).contentType.toLowerCase()) != -1;
        }

        if (isIFrameableResource(resource)) {
            var templateUrl = ServiceLookup.getUrlFor('previewFile');
            var file = defaultFile(resource);
            var key = "";

            if (file.content) {
                key = $routeParams.itemId + "/" + resource.name + "/" + file.name;
            } else {
                key = file.storageKey;
            }
            $scope.currentHtmlUrl = templateUrl.replace("{key}", key);
            $scope.currentMaterial = resource;
        } else {
            throw "Can't preview file";
        }
    };

    $scope.createNewHtmlResource = function () {

        $scope.supportingMaterials = ($scope.supportingMaterials || []);

        var newName = $scope.newResourceName == "Other" ? $scope.newResourceOtherName : $scope.newResourceName;


        var newHtml = new SupportingMaterial(
            {
                name:newName,
                files:[
                    {
                        name:"index.html",
                        content:"<html><body>hello world</body></html>",
                        default:true,
                        contentType:"text/html"
                    }
                ]
            }
        );

        newHtml.$save({ itemId:$routeParams.itemId }, function (data) {
            $scope.supportingMaterials.push(data);
            $scope.showResource(data);
            $scope.showAddResourceModal = false;
        });
    };

    $scope.calculateResourceUploadUrl = function (file) {

        $scope.showAddResourceModal = false;

        if (file == null) {
            throw "ItemController:calculateResourceUploadUrl - the file is null"
        }
        return $scope.getUrl("uploadSupportingMaterial", $routeParams.itemId, $scope.getResourceFileName());
    };

    $scope.getResourceFileName = function () {
        if ($scope.newResourceName == "Other") {
            return $scope.newResourceOtherName;
        }
        return $scope.newResourceName;
    };

    $scope.showEditButton = function (resource) {
        function isSingleStoreFile(resource) {
            return resource.files && resource.files.length == 1 && resource.files[0].storageKey;
        }

        return !isSingleStoreFile(resource);
    };

    /**
     * Result callback from the file upload
     * @param result - the json from the server with the StoredFile properties.
     */
    $scope.onResourceUploadCompleted = function (result) {
        console.log("onResourceUploadCompleted!!: " + result);

        $scope.$apply(function () {
            var resultObject = $.parseJSON(result);

            if (!$scope.supportingMaterials) {
                $scope.supportingMaterials = [];
            }

            $scope.supportingMaterials.push(resultObject);
            $scope.showResource(resultObject);
            //no need to save - its already been saved by the upload
        });
    };

    $scope.editResource = function (resource) {

        var urls = {};

        var substitutions = {
            itemId: $routeParams.itemId,
            resourceName: resource.name
        };

        urls.uploadFile = ServiceLookup.getUrlFor('uploadSupportingMaterialFile', substitutions);
        urls.createFile = ServiceLookup.getUrlFor('createSupportingMaterialFile', { itemId: $routeParams.itemId});
        urls.updateFile = ServiceLookup.getUrlFor('updateSupportingMaterialFile', substitutions);
        urls.deleteFile = ServiceLookup.getUrlFor('deleteSupportingMaterialFile', substitutions);

        $rootScope.$broadcast('enterEditor', resource, true, urls);
    };

    $scope.$on('enterEditor', function(event){
       $scope.isEditorActive = true;
    });

    $scope.$on('leaveEditor', function(event){
        $scope.isEditorActive = false;
    });

    $scope.removeResource = function (resource) {

        if ($scope.supportingMaterials == null) {
            throw "Can't remove from null array";
        }

        if (!resource) {
            return;
        }

        resource.$delete({itemId:$routeParams.itemId, resourceName:resource.name}, function (data) {
            $scope.supportingMaterials.removeItem(data);
        });
    };

    //TODO: dup from ItemController
    $scope.getUrl = function (action, itemId, fileName) {
        var templateUrl = ServiceLookup.getUrlFor(action);

        if (templateUrl == null) {
            throw "Can't find url for action: " + action;
        }

        return templateUrl.replace("{itemId}", itemId).replace("{fileName}", fileName)
    };

    $scope.loadMaterials = function () {
        SupportingMaterial.query(
            {
                itemId:$routeParams.itemId,
                access_token:AccessToken.token
            },
            function onLoaded(data) {
                $scope.supportingMaterials = data;
            });
    };

    $scope.loadMaterials();
}

SupportingMaterialsController.$inject = [
    '$scope',
    '$rootScope',
    '$routeParams',
    'SupportingMaterial',
    'ServiceLookup',
    'AccessToken'];
