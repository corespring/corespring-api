qtiDirectives.directive("textentryinteraction", function (QtiUtils) {


    return {
        restrict:'E',
        replace:true,
        scope:true,
        require:'^assessmentitem',
        template:'<span class="text-entry-interaction"><input type="text" size="{{expectedLength}}" ng-model="textResponse" ng-disabled="formDisabled"></input></span>',
        link:function (scope, element, attrs, AssessmentItemController) {
            var responseIdentifier = attrs.responseidentifier;

            scope.expectedLength = attrs.expectedlength;

            // called when this choice is clicked
            scope.$watch('textResponse', function (newVal, oldVal) {
                AssessmentItemController.setResponse(responseIdentifier, scope.textResponse);
            });

            scope.$watch('status', function (newValue) {

                if (newValue != 'SUBMITTED' || scope.isFeedbackEnabled() == false) {
                    return;
                }

                var correctResponse = scope.itemSession.sessionData.correctResponses[responseIdentifier];
                var className = QtiUtils.compare(scope.textResponse, correctResponse) ? 'correct-response' : 'incorrect-response';
                element.toggleClass(className);
            });
        }
    }
});