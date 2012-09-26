function ViewItemController($scope, $routeParams, $location, ItemService, AccessToken){
    
    /**
     * Update the location search settings to reflect the ui state
     * Generates: ?preview=[1|0]&panel=[content|metadata]
     * @param panelName
     * @param previewVisible
     */
    function updateLocation(panelName, previewVisible, fileListVisible) {
        var current = $location.search();
        var previewNumber = previewVisible ? "1" : "0";
        var fileListNumber = fileListVisible ? "1" : "0";

        if (current.panel == panelName
            &&
            current.preview == previewNumber
            &&
            current.fileList == fileListNumber) {
            return;
        }
        $location.search("panel=" + panelName + "&preview=" + previewNumber + "&fileList=" + fileListNumber);
    }


   function initPane($routeParams) {
        var panelName = 'item';
        if ($routeParams.panel) {
            panelName = $routeParams.panel;
        }
        $scope.changePanel(panelName);
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
        updateLocation($scope.currentPanel, $scope.previewVisible);
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
