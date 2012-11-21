
qtiDirectives.directive('choiceinteraction', function () {

    return {
        restrict: 'E',
        transclude: true,
        template: '<div class="choice-interaction" ng-transclude="true"></div>',
        replace: true
    }
});

