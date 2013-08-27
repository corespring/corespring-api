/**
 * Shared function for handling feedback blocks
 * @return {Object}
 */
var feedbackDirectiveFunction = function ($compile, QtiUtils) {

    return {
        restrict:'ACE',
        template:'<span class="{{cssClass}}"></span>',
        scope:true,
        require:'^assessmentitem',
        link:function (scope, element, attrs) {

            var csFeedbackId = attrs["csfeedbackid"];

            scope.$on('resetUI', function (event) {
                scope.feedback = "";
            });

            scope.cssClass = element[0].localName;

            scope.$watch('itemSession.sessionData.correctResponses', function (responses) {

                if(!responses || !scope.isFeedbackEnabled()) return;

                var feedback = scope.itemSession.sessionData.feedbackContents[csFeedbackId];
                scope.feedback = ( feedback || "" );

                element.html(feedback || "")
                $compile(element.contents())(scope)

              setTimeout(function () {
                if (typeof(MathJax) != "undefined") {
                  MathJax.Hub.Queue(["Typeset", MathJax.Hub]);
                }
              }, 10);

            });
            scope.feedback = "";
        },
        controller:function ($scope) {
            this.scope = $scope;
        }
    }
};

angular.module('qti.directives').directive('feedbackblock', ['$compile', feedbackDirectiveFunction]);

angular.module('qti.directives').directive('feedbackinline', ['$compile', feedbackDirectiveFunction]);

angular.module('qti.directives').directive('modalfeedback', ['$compile', feedbackDirectiveFunction]);
