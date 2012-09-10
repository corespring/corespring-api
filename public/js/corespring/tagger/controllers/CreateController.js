/*
 * Controller for creating new item, in practice after the object is persisted
 * Control moves to the EditCtrl
 */
function CreateCtrl($scope, $location, ItemData, $routeParams, NewItemTemplates) {

    $scope.$root.$broadcast("createNewItem");

    var getXmlDataForType = function (itemType) {
        if (NewItemTemplates == null || NewItemTemplates[itemType] == null) {
            console.warn("couldn't find qtiTemplates for type: " + itemType);
            return ""
        }
        return NewItemTemplates[itemType].xmlData
    };

    /**
     * Create a default item data object.
     */
    function createDefaultItemData(itemType) {
        var itemData = new ItemData();
        itemData.xmlData = getXmlDataForType(itemType);
        return itemData;
    }

    $scope.itemData = createDefaultItemData($routeParams.type);

    ItemData.save($scope.itemData, function (itemData) {
        $location.path('/edit/' + itemData._id.$oid);
    });
}

CreateCtrl.$inject = ['$scope', '$location', 'ItemData', '$routeParams', 'NewItemTemplates'];
