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
                    if (feedback) {
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


qtiDirectives.directive('feedbackblock', feedbackDirectiveFunction);

qtiDirectives.directive('feedbackinline', feedbackDirectiveFunction);

qtiDirectives.directive('modalfeedback', feedbackDirectiveFunction);
