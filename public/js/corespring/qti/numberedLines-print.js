qtiDirectives.directive('line', function () {
        return {
            restrict:'E',
            transclude:true,
            template:"<li ng-transclude />"
        }
    }
);

qtiDirectives.directive('numberedLines', function () {
    return {
        restrict:'C',
        scope:true,
        transclude: true,
        template: "<ol ng-transclude />"
    }
});