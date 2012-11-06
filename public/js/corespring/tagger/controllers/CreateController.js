/*
 * Controller for creating new item, in practice after the object is persisted
 * Control moves to the EditCtrl
 */
function CreateCtrl($scope, ItemService, NewItemTemplates, Collection, AccessToken) {

    $scope.$root.$broadcast("createNewItem");

    $scope.templates = [];
    for (var t in NewItemTemplates) $scope.templates.push(NewItemTemplates[t]);
    $scope.selectedTemplate = $scope.templates[0];

    // TODO: this should be filtered so that only writable collections appear
    $scope.collections = Collection.query({ access_token : AccessToken.token },
        function() {
            console.log("Done");
            $scope.selectedCollection = $scope.collections[0];
        });


    $scope.createItem = function() {
        if (angular.isUndefined($scope.selectedCollection) || angular.isUndefined($scope.selectedTemplate))
            return false;

        var item = new ItemService();
        item.collectionId = $scope.selectedCollection.id;
        item.data = {
            name: "qtiItem",
            files: [{
                name: "qti.xml",
                "default": true,
                contentType: "text/xml",
                content: $scope.selectedTemplate.xmlData
            }]
        };

        item.$save({access_token: AccessToken.token},
            function onItemSaved(itemData) {
                console.log("Item Saved");
                console.log(itemData);
                window.location.href = '/web#/edit/' + itemData.id;
            },
            function onError(e) {
                alert("Error Saving Item: "+ e.data.message);
            }
        );

        return item;

    }
}

CreateCtrl.$inject = ['$scope', 'ItemService','NewItemTemplates','Collection','AccessToken'];
