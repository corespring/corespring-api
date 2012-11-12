function ViewItemController($scope, $routeParams, $location, ItemService, AccessToken) {

    /**
     * Update the location search settings to reflect the ui state
     * Generates: ?panel=[content|metadata]
     * @param panelName
     */
    function updateLocation(panelName) {
        var current = $location.search();

        if (current.panel == panelName) {
            return;
        }
        $location.search("panel=" + panelName);
    }


    function initPane($routeParams) {

        $scope.$root.mode = 'view';

        var panelName = 'item';
        if ($routeParams.panel) {
            panelName = $routeParams.panel;
        }
        $scope.changePanel(panelName);

        $scope.$watch(
            function () {
                return $location.url();
            },
            function (path) {
                $scope.changePanel($location.search().panel);
            });

    }

    $scope.loadItem = function () {
        ItemService.get({id:$routeParams.itemId, access_token:AccessToken.token}, function onItemLoaded(itemData) {
            $scope.itemData = itemData;
        });
    };


    $scope.editItem = function () {
        $location.url('/edit/' + $scope.itemData.id);
    };

    $scope.changePanel = function (panelName) {
        $scope.currentPanel = panelName;
        $scope.$broadcast("tabSelected");
        updateLocation($scope.currentPanel);
    };

    $scope.getItemUrl = function () {
        if (!$scope.itemData) return null;
        return "/web/show-resource/" + $scope.itemData.id;
    };

    $scope.prependHttp = function (url) {
        if (!url) return "";
        if (!url.match(/^[a-zA-Z]+:\/\//)) {
            url = 'http://' + url;
        }
        return url;
    };


    $scope.loadItem();
    initPane($routeParams);
}

ViewItemController.$inject = [
    '$scope',
    '$routeParams',
    '$location',
    'ItemService',
    'AccessToken'
];
