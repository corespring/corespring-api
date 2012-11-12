/*
 * Controller for creating new item, in practice after the object is persisted
 * Control moves to the EditCtrl
 */
function CreateCtrl($scope, $routeParams, ItemService, NewItemTemplates, Collection, AccessToken) {

    $scope.$root.$broadcast("createNewItem");

    if (angular.isUndefined($routeParams.type) || angular.isUndefined(NewItemTemplates[$routeParams.type]))
        return false;

    var item = new ItemService();
    item.data = {
        name: "qtiItem",
        files: [{
            name: "qti.xml",
            "default": true,
            contentType: "text/xml",
            content: NewItemTemplates[$routeParams.type].xmlData
        }]
    };

    console.log(item);
//
//    item.$save({},
//        function onItemSaved(itemData) {
//            window.location.href = '/web#/edit/' + itemData.id;
//        },
//        function onError(e) {
//            alert("Error Saving Item: "+ e.data.message);
//        }
//    );

}

CreateCtrl.$inject = ['$scope', '$routeParams','ItemService','NewItemTemplates','Collection','AccessToken'];
