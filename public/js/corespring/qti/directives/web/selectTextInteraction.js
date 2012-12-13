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
            };
            $scope.removeSelection = function (selection) {
                var newSelections = [];
                for (var i = 0; i < $scope.selections.length; i++)
                    if ($scope.selections[i] != selection) newSelections.push($scope.selections[i]);
                $scope.selections = newSelections;
                console.log($scope.selections);
            };
        },
        compile: function compile(tElement, tAttrs) {
            var sentenceRegExp = /\n|([^\r\n.!?]+([.!?]+|$))/gim;
            var wordRegExp = /\b[a-zA-Z_']+\b/gim;
            var regExp = (tAttrs.selectiontype == "word") ? wordRegExp : sentenceRegExp;
            var html = tElement.html();
            tElement.html(html.replace(regExp, function (match, b1, b2) {
                if (tAttrs.selectiontype == "word" && "</".indexOf(b2.charAt(b1-1)) >= 0)
                    return match
                else
                    return "<span class='selectable'>" + match + "</span>";
            }));
            return function link(scope, iElement, attrs, AssessmentItemController) {
                var responseIdentifier = attrs.responseidentifier;
                scope.controller = AssessmentItemController;
                scope.$watch('selections.length', function () {
                    console.log("Selection changed");
                    scope.controller.setResponse(responseIdentifier, scope.selections);
                });
            }
        }
    };
});

qtiDirectives.directive('selectable', function factory() {
    return {
        restrict: 'C',
        scope: true,
        requires: "^selectTextInteraction",
        compile: function compile(tElement, tAttrs) {
            var trimmedHtml = tElement.html().replace(/^[^a-zA-Z]*?([a-zA-Z])/, "$1").replace(/([a-zA-Z])[^a-zA-Z]*?$/,"$1");
            tElement.html("<span ng-click=\"toggle()\" ng-class=\"{selected: isSelected}\">"+tElement.html()+"</span>");
            return function link(scope, iElement, iAttrs) {
                scope.isSelected = false;
                scope.toggle = function () {
                    scope.isSelected = !scope.isSelected;
                    if (scope.isSelected)
                        scope.addSelection(trimmedHtml);
                    else
                        scope.removeSelection(trimmedHtml);
                }
            }
        }
    };
});