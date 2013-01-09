function PreviewController($scope, $timeout, Config, Item, ServiceLookup) {

    $scope.changeSupportingMaterialPanel = function (sm) {
        $scope.changePanel(sm.name);
        $scope.currentSm = sm;
    };


    $scope.getItemSrc = function (forPrinting) {
        var templateUrl = ServiceLookup.getUrlFor(forPrinting ? 'printResource' : 'renderResource');
        var key = $scope.itemData.id;
        return templateUrl.replace("{key}", key);
    };

    $scope.getSmSrc = function (sm, forPrinting) {
        //var templateUrl = ServiceLookup.getUrlFor('previewFile');
        var templateUrl = ServiceLookup.getUrlFor(forPrinting ? 'printResource' : 'renderResource');
        var key = $scope.itemData.id + "/" + sm.name;
        //empty it so we trigger a refresh
        return templateUrl.replace("{key}", key);
    };

    $scope.printCurrent = function () {

        var features = "width=650,height=800,menubar=yes,location=yes,resizable=yes,scrollbars=yes,status=yes";

        function getPrintUrl(panel) {
            switch(panel){
                case "profile" : return ServiceLookup.getUrlFor('printProfile').replace("{key}", $scope.itemData.id);
                case "item" : return $scope.getItemSrc(true);
                default : return $scope.getSmSrc($scope.currentSm, true);
            }
        }
        var url = getPrintUrl($scope.currentPanel);

        var newWindow = window.open(url, 'name', features);

        if(newWindow){
            newWindow.focus();
        }
    };

    $scope.changePanel = function (panelName) {
        $scope.currentSm = null;
        $scope.currentPanel = panelName;
    };

    $scope.getItemUrl = function() {
        if (!$scope.itemData || $scope.currentPanel != 'item') return null;
        return "/web/show-resource/" + $scope.itemData.id + "/data/main";
    }

    $scope.prependHttp = function(url) {
        if (!url) return "";
        if (!url.match(/^[a-zA-Z]+:\/\//))
        {
            url = 'http://' + url;
        }
        return url;
    }

    $scope.loadItem = function () {
        Item.get(
            {
                id:Config.itemId
            },
            function onItemLoaded(itemData) {
                $scope.itemData = itemData;

                $timeout(function () {
                    MathJax.Hub.Queue(["Typeset", MathJax.Hub]);
                }, 200);
            }
        );
    };

    $scope.itemId = Config.itemId;
    $scope.loadItem();
    $scope.currentPanel = "profile";
}

PreviewController.$inject = ['$scope', '$timeout', 'Config', 'Item', 'ServiceLookup'];