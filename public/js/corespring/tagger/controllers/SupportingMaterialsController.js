/**
 */
function SupportingMaterialsController($scope, $routeParams, ItemService, ServiceLookup, AccessToken) {

    $scope.showAddResourceModal = false;
    $scope.newSmType = "upload";
    $scope.newSmName = "Rubric";
    $scope.newSmOtherName = "";

    $scope.canCreateNewSm = function(){
       return $scope.newSmName == "Other" && !$scope.newSmOtherName;
    };

    $scope.currentFile = null;
    $scope.itemId = $routeParams.itemId;

    $scope.showSm = function(sm) {

       if( sm.files ){

           var templateUrl = ServiceLookup.getUrlFor('supportingMaterialRunner');

           var finalUrl =
               templateUrl
               .replace("{itemId}", $routeParams.itemId)
               .replace("{materialName}", sm.name)
               .replace("{mainFileName}", sm.files[0].name );

           $scope.currentHtmlUrl = finalUrl;
           $scope.currentFile = null;
       } else {
           $scope.currentHtmlUrl = null;
           $scope.currentFile = sm.name;
       }
       $scope.currentMaterial = sm;
    };

    $scope.createNewHtmlSm = function(){

        $scope.itemData.supportingMaterials = ($scope.itemData.supportingMaterials || []);

        var blankHtml = { name: $scope.newSmName, inlineFiles : [
            { name: "index.html", content: "<html><body>hello world</body></html>"}
        ] };
        $scope.itemData.supportingMaterials.push( blankHtml );

        $scope.showAddResourceModal = false;

        $scope.showSm(blankHtml);
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

    $scope.getSmFileName = function(){
        if( $scope.newSmName == "Other") {
            return $scope.newSmOtherName;
        }
        return $scope.newSmName;
    };

    $scope.onSmUploadCompleted = function(result){
        console.log("onSmUploadCompleted!!: " + result);

        $scope.$apply(function(){
            var resultObject = $.parseJSON(result);

            if( !$scope.itemData.supportingMaterials ){
                $scope.itemData.supportingMaterials = [];
            }

            $scope.itemData.supportingMaterials.push(resultObject);
            $scope.showSm(resultObject);
            //no need to save - its already been saved by the upload
        });
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
