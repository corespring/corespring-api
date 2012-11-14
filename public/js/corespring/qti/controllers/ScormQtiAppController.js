function QtiAppController($scope, $timeout, $location, AssessmentSessionService) {

    $timeout(function () {
        if (typeof(MathJax) != "undefined") {
            MathJax.Hub.Queue(["Typeset", MathJax.Hub]);
        }
    }, 200);

    $scope.init = function () {
        var url = $location.absUrl();
        var matches = url.match(/.*\/scorm-player\/(.*?)\/.*/);
        var params = { itemId:matches[1] };
        AssessmentSessionService.create(params, {}, function (data) {
            $scope.itemSession = data;
        });
    };

    $scope.init();
}

QtiAppController.$inject = ['$scope', '$timeout', '$location', 'AssessmentSessionService'];

