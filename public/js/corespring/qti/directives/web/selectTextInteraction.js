qtiDirectives.directive('selecttextinteraction', function factory() {
    return {
        restrict: 'ACE',
        scope: true,
        require:'^assessmentitem',
        controller: function ($scope) {
            $scope.selections = [];
            $scope.addSelection = function (selection) {
                $scope.selections.push(selection);
                console.log($scope.selections);
            }
            $scope.removeSelection = function (selection) {
                var newSelections = [];
                for (var i = 0; i < $scope.selections.length; i++)
                    if ($scope.selections[i] != selection) newSelections.push($scope.selections[i]);
                $scope.selections = newSelections;
                console.log($scope.selections);
            }
        },
        link: function(scope, element, attrs, AssessmentItemController) {
            scope.controller = AssessmentItemController;
            var responseIdentifier = attrs.responseidentifier;
            scope.$watch('selections.length', function () {
                scope.controller.setResponse(responseIdentifier, scope.selections);
                scope.noResponse = false;//(scope.isEmptyItem(scope.selections) && scope.showNoResponseFeedback);
            });
        }
    };
});

qtiDirectives.directive('selectable', function factory() {
    return {
        restrict: 'ACE',
        scope: true,
        replace: false,
        template: "<span ng-click='toggle()' ng-class='{selected: isSelected}' ng-transclude></span>",
        transclude: true,
        require: "^selecttextinteraction",
        compile: function compile(tElement, tAttrs) {
            var outerHtml = tElement[0].outerHTML;

            return function link(scope, iElement, iAttrs) {
                scope.isSelected = false;
                scope.id = /id=".([0-9]+)"/gim.exec(outerHtml)[1];
                scope.toggle = function () {
                    scope.isSelected = !scope.isSelected;
                    if (scope.isSelected)
                        scope.addSelection(scope.id);
                    else
                        scope.removeSelection(scope.id);
                }
            }
        }
    };
});