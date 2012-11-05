qtiDirectives.directive("textentryinteraction", function (QtiUtils) {


    return {
        restrict:'E',
        replace:true,
        scope:true,
        require:'^assessmentitem',
        template:'<span class="text-entry-interaction" ng-class="{noResponse: noResponse}"><input type="text" size="{{expectedLength}}" ng-model="textResponse" ng-disabled="formDisabled"></input></span>',
        link:function (scope, element, attrs, AssessmentItemController) {
            var responseIdentifier = attrs.responseidentifier;
            scope.controller = AssessmentItemController;

            scope.CSS = { correct:'correct-response', incorrect:'incorrect-response'};

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
                    .removeClass(scope.CSS.correct)
                    .removeClass(scope.CSS.incorrect);
            };

            scope.$on('resetUI', function (event) {
                removeCss();
                scope.textResponse = "";
            });

            scope.$watch('itemSession.responses', function (responses) {
                if (!responses) return;

                removeCss();

                var response = QtiUtils.getResponseById(responseIdentifier, scope.itemSession.responses);

                if(!response || response.value == "" || response.value == null){
                    return;
                }

                var isCorrect = QtiUtils.isResponseCorrect(response);


                if (isCorrect) {
                    if (scope.highlightCorrectResponse() || scope.highlightUserResponse()) {
                        element.addClass(scope.CSS.correct);
                    }
                } else {
                    if (scope.highlightUserResponse()) {
                        element.addClass(scope.CSS.incorrect);
                    }
                }
            });
        }
    }
});