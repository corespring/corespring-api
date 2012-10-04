qtiDirectives.directive("textentryinteraction", function (QtiUtils) {


    return {
        restrict:'E',
        replace:true,
        scope:true,
        require:'^assessmentitem',
        template:'<span class="text-entry-interaction" ng-class="{noResponse: noResponse}"><input type="text" size="{{expectedLength}}" ng-model="textResponse" ng-disabled="formDisabled"></input></span>',
        link:function (scope, element, attrs, AssessmentItemController) {
            var responseIdentifier = attrs.responseidentifier;

            scope.expectedLength = attrs.expectedlength;

            scope.$watch('textResponse', function () {
                AssessmentItemController.setResponse(responseIdentifier, scope.textResponse);
                scope.noResponse = (scope.isEmptyItem(scope.textResponse) && scope.showNoResponseFeedback);
            });

            scope.$watch('showNoResponseFeedback', function(newVal, oldVal) {
                scope.noResponse = (scope.isEmptyItem(scope.textResponse) && scope.showNoResponseFeedback);
            });

            scope.$on('submitResponses', function (event) {
                element
                    .removeClass('correct-response')
                    .removeClass('incorrect-response');
            });

            var isCorrect = function (value) {
                return QtiUtils.compare(scope.textResponse, value);
            };

            scope.$watch('itemSession.sessionData.correctResponses', function (responses) {
                if (!responses) return;
                if (!scope.isFeedbackEnabled()) return;

                var correctResponse = responses[responseIdentifier];
                var className = isCorrect(correctResponse) ? 'correct-response' : 'incorrect-response';
                element.toggleClass(className);
            });
        }
    }
});