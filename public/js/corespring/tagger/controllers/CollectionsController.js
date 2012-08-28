function CollectionsCtrl($scope, Collection) {

    $scope.$root.mode = "collections";

    $scope.collections = Collection.query();

    $scope.save = function () {
        Collection.save($scope.collection, function (collection) {
            $scope.collections = Collection.query();
            $scope.collection.name = "";
            $scope.collection.description = "";
        });
    }
}
CollectionsCtrl.$inject = ['$scope', 'Collection'];
