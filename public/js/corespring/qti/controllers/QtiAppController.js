function QtiAppController($scope, $timeout, $location, AssessmentSessionService) {

    $timeout(function () {
        if (typeof(MathJax) != "undefined") {
            MathJax.Hub.Queue(["Typeset", MathJax.Hub]);
        }
    }, 200);

    $scope.reset = function () {
        $scope.$broadcast('reset');
    };

    $scope.init = function () {
        var url = $location.absUrl();
        var matches = url.match(/.*\/item\/(.*?)\/.*/);
        var params = { itemId:matches[1] };
        AssessmentSessionService.create(params, {}, function (data) {
            $scope.itemSession = data;
            $scope.setUpChangeWatcher();
            $scope.settingsHaveChanged = false;
        });
    };

    /**
     * Track changes to settings so we know if the user needs to save the changes
     * before working with the item.
     */
    $scope.setUpChangeWatcher = function() {

        $scope.originalSettings = angular.copy($scope.itemSession.settings);
        $scope.maxNoOfAttempts = $scope.itemSession.settings.maxNoOfAttempts;

        //need to make sure we store an int from the radio group
        $scope.$watch('itemSession.settings.maxNoOfAttempts', function(newData){
            $scope.itemSession.settings.maxNoOfAttempts = parseInt(newData);
        });

        //watcher for $watch - builds string from object values
        var watcher = function() {
            var out = "";
            for(var x in $scope.itemSession.settings){
                out += $scope.itemSession.settings[x];
            }
            return out;
        };

        $scope.$watch( watcher, function(newData){
            $scope.settingsHaveChanged = !angular.equals(
                $scope.originalSettings,
                $scope.itemSession.settings);
        });

    };

    /**
     * Because the current item session has been started - its settings are now locked.
     * So we are going to be creating a new item session.
     */
    $scope.reloadItem = function () {
        AssessmentSessionService.create({itemId:$scope.itemSession.itemId}, $scope.itemSession, function (data) {
            $scope.reset();
            $scope.$broadcast('unsetSelection');
            $scope.itemSession = data;
            $scope.setUpChangeWatcher();
            // Empty out the responses
            for (var i = 0; i < $scope.responses.length; i++)
                $scope.responses[i].value = [];
        });
    };

    $scope.init();
}

QtiAppController.$inject = ['$scope', '$timeout', '$location', 'AssessmentSessionService'];

