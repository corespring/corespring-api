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

        var newWindow = null;

        if ($scope.currentPanel == "profile") {
            newWindow = window.open(ServiceLookup.getUrlFor('printProfile').replace("{key}", $scope.itemData.id), 'name', 'height=600,width=800');
        } else if ($scope.currentPanel == "item") {
            newWindow = window.open($scope.getItemSrc(true), 'name', 'height=600,width=800');
        } else if ($scope.currentSm) {
            newWindow = window.open($scope.getSmSrc($scope.currentSm, true), 'name', 'height=600,width=800');
        }

        if(newWindow){
            newWindow.focus();
        }
    };

    $scope.changePanel = function (panelName) {
        $scope.currentSm = null;
        $scope.currentPanel = panelName;
    };

    $scope.getGradeLevelCssName = function (gradeLevel){
        return "grade-" + gradeLevel;
    };

    $scope.getItemUrl = function() {
        if (!$scope.itemData) return null;
        return "/web/show-resource/" + $scope.itemData.id;
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
                id:Config.itemId,
                access_token:Config.accessToken
            },
            function onItemLoaded(itemData) {
                console.log("ItemLoaded!");
                $scope.itemData = itemData;

                $timeout(function () {
                    MathJax.Hub.Queue(["Typeset", MathJax.Hub]);
                }, 200);
            }
        );
    };


    $scope.itemId = Config.itemId;
    $scope.loadItem();
    $scope.currentPanel = "item";
}

PreviewController.$inject = ['$scope', '$timeout', 'Config', 'Item', 'ServiceLookup'];