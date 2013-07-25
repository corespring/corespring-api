angular.module('qti.directives').directive("textentryinteraction", function (QtiUtils) {


    return {
        restrict: 'E',
        replace: true,
        scope: true,
        require: '^assessmentitem',
        template: '<span class="text-entry-interaction" ng-class="{noResponse: noResponse}"><input type="text" size="{{expectedLength}}" ng-model="textResponse" ng-disabled="formSubmitted"></input></span>',
        link: function (scope, element, attrs, AssessmentItemController) {
            var responseIdentifier = attrs.responseidentifier;
            scope.controller = AssessmentItemController;

            scope.controller.registerInteraction(element.attr('responseIdentifier'), "text entry", "fill-in");

            scope.CSS = { correct: 'correct-response', incorrect: 'incorrect-response', received: 'received-response' };

            scope.expectedLength = attrs.expectedlength;

            scope.$watch('textResponse', function () {
                scope.controller.setResponse(responseIdentifier, scope.textResponse);
                scope.noResponse = (scope.isEmptyItem(scope.textResponse) && scope.showNoResponseFeedback);
            });

            scope.$watch('showNoResponseFeedback', function (newVal, oldVal) {
                scope.noResponse = (scope.isEmptyItem(scope.textResponse) && scope.showNoResponseFeedback);
            });


            var removeCss = function () {
                element
                    .removeClass(scope.CSS.received)
                    .removeClass(scope.CSS.correct)
                    .removeClass(scope.CSS.incorrect);
            };

            scope.$on('resetUI', function () {
                removeCss();
            });

            scope.$on('unsetSelection', function () {
                scope.textResponse = "";
            });

            scope.$on('highlightUserResponses', function () {
                scope.textResponse = QtiUtils.getResponseValue(responseIdentifier, scope.itemSession.responses, "");
            });

            var isCorrect = function (value) {
                return QtiUtils.compare(scope.textResponse, value);
            };

            scope.$watch('itemSession.sessionData.correctResponses', function (responses) {
                if (!responses) return;

                var correctResponse = QtiUtils.getResponseValue(responseIdentifier, responses, "");
                var outcome = QtiUtils.getOutcomeValue(responseIdentifier,scope.itemSession.responses)
                removeCss();

                if (responses.length == 0) {
                    element.addClass(scope.CSS.received);
                }
                else if (isCorrect(correctResponse) || (outcome && outcome.isCorrect)) {
                    if (scope.highlightCorrectResponse() || scope.highlightUserResponse()) {
                        element.addClass(scope.CSS.correct);
                    }
                }
                else {
                    if (scope.highlightUserResponse()) {
                        element.addClass(scope.CSS.incorrect);
                    }
                }
            });
        }
    }
});