angular.module('qti.directives').directive('line', function () {
        return {
            restrict:'E',
            transclude:true,
            template:"<li ng-transclude />"
        }
    }
);

angular.module('qti.directives').directive('numberedLines', function () {
    return {
        restrict:'C',
        scope:true,
        transclude: true,
        template: "<ol ng-transclude />"
    }
});