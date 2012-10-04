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
        link:function (scope, element, attrs) {

            var csFeedbackId = attrs["csfeedbackid"];

            scope.$on('submitResponses', function (event) {
                scope.feedback = "";
            });

            scope.cssClass = element[0].localName;

            scope.$watch('itemSession.sessionData.correctResponses', function (responses) {
                if (!responses || scope.isFeedbackEnabled() == false) return;

                var feedback = scope.itemSession.sessionData.feedbackContents[csFeedbackId];
                if (feedback) {
                    scope.feedback = feedback;
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
