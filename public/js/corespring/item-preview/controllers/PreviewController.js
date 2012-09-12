function PreviewController($scope, $timeout, Config, Item, ServiceLookup) {


    function defaultFile(resource) {
        return _.find(resource.files, function (f) {
            return f['default'] == true;
        });
    }


    $scope.changeSupportingMaterialPanel = function (sm) {
        $scope.changePanel(sm.name);
        $scope.currentSm = sm;
    };


    $scope.getSmSrc = function (sm) {
        var templateUrl = ServiceLookup.getUrlFor('previewFile');
        var file = defaultFile(sm);
        if(!file) throw "Can't find default file";
        var key = $scope.itemData.id + "/" + sm.name + "/" + file.name;
        //empty it so we trigger a refresh
        return templateUrl.replace("{key}", key);
    };

    $scope.changePanel = function (panelName) {
        $scope.currentSm = null;
        $scope.currentPanel = panelName;
    };

    $scope.loadItem = function () {
        Item.get(
            {
                id:Config.itemId,
                access_token:Config.accessToken
            },
            function onItemLoaded(itemData) {
                console.log("ItemLoaded!");
                $scope.itemData = itemData;
            }
        );
    };

    $scope.loadItem();
    $scope.currentPanel = "item";
}

PreviewController.$inject = ['$scope', '$timeout', 'Config', 'Item', 'ServiceLookup'];