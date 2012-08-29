/**
 */
function SupportingMaterialsController($scope, $routeParams, ItemService, ServiceLookup, AccessToken) {

    $scope.currentFile = null;
    $scope.itemId = $routeParams.itemId;

    $scope.showFile = function(file) {
       $scope.currentFile = file.name;
    };

    $scope.calculateSupportingMaterialUploadUrl = function (file) {
        if (file == null) {
            throw "ItemController:calculateSupportingMaterialUploadUrl - the file is null"
        }
        return $scope.getUrl("uploadSupportingMaterial", $routeParams.itemId, file.name);
    };

    $scope.onSupportingMaterialUploadCompleted = function(result){
        console.log("onSupportingMaterialUploadCompleted!!: " + result);

        $scope.$apply(function(){
            var resultObject = $.parseJSON(result);

            if( !$scope.itemData.supportingMaterials ){
                $scope.itemData.supportingMaterials = [];
            }
            $scope.itemData.supportingMaterials.push(resultObject);
            console.log("now: " + $scope.itemData.supportingMaterials.length);
            //no need to save - its already been saved by the upload
        });
    };


    $scope.removeFile = function (file) {

        if ($scope.itemData.supportingMaterials == null) {
            throw "Can't remove from null array";
        }
        var result = $scope.itemData.supportingMaterials.removeItem(file);

        if (result == file) {

            var deleteUrl = $scope.getUrl("deleteSupportingMaterial", $scope.itemId, file.file);

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
                type: "DELETE",
                url: deleteUrl,
                data: {}
            }).done(onDeleteSuccess);


            $scope.save();
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

    /*$scope.save = function(){

        if(!$scope.itemData){
            return;
        }
        $scope.itemData.update({access_token: AccessToken.token},function (data) {});
    };*/

    $scope.loadItem = function(){
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
    '$routeParams',
    'ItemService',
    'ServiceLookup',
    'AccessToken'];
