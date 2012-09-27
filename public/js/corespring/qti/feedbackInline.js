/**
 * Shared function for handling feedback blocks
 * @return {Object}
 */
var feedbackDirectiveFunction = function (QtiUtils) {

    return {
        restrict:'E',
        template:'<span class="{{cssClass}}" ng-bind-html-unsafe="feedback"></span>',
        scope:true,
        require:'^assessmentitem',
        link:function (scope, element, attrs, AssessmentItemCtrl, $timeout) {
            scope.cssClass = element[0].localName;
            var csFeedbackId = attrs["csfeedbackid"];

            scope.$watch('status', function (newValue, oldValue) {
                if (scope.isFeedbackEnabled() == false) return; // break if feedback is disabled
                if (newValue == 'SUBMITTED') {
                    var feedback = scope.itemSession.sessionData.feedbackContents[csFeedbackId];
                    var outcomeIdentifier = attrs["outcomeidentifier"];

                    if (!outcomeIdentifier) return;
                    var responseIdentifier = outcomeIdentifier.match(/\.(.*?)\./)[1];
                    var correctResponse = scope.itemSession.sessionData.correctResponses[responseIdentifier];
                    var choiceValue = attrs["identifier"];
                    var response = QtiUtils.getResponseById(responseIdentifier, scope.itemSession.responses);// localScope.itemSession.responses[responseIdentifier].value;
                    var responseValue = response ? response.value : "";
                    console.log("choiceValue: " + choiceValue);
                    if (QtiUtils.compare(choiceValue, correctResponse) || QtiUtils.compare(choiceValue, responseValue)) {
                        scope.feedback = feedback;
                    }
                }
            });
            scope.feedback = "";
        },
        controller:function ($scope) {
            this.scope = $scope;
        }
    }
};

qtiDirectives.directive('feedbackinline', feedbackDirectiveFunction);

qtiDirectives.directive('modalfeedback', feedbackDirectiveFunction);
