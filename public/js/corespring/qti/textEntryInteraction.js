qtiDirectives.directive("textentryinteraction", function(QtiUtils) {


    return {
        restrict: 'E',
        replace: true,
        scope: true,
        require: '^assessmentitem',
        template: '<span><input type="text" size="{{expectedLength}}" ng-model="textResponse" ng-disabled="formDisabled"></input></span>',
        link: function (scope, element, attrs, AssessmentItemController) {
            // read some stuff from attrs
            var modelToUpdate = attrs.responseidentifier;
            var responseIdentifier = attrs.responseIdentifier;

            scope.expectedLength = attrs.expectedlength;

            // called when this choice is clicked
            scope.$watch('textResponse', function(newVal, oldVal) {
                AssessmentItemController.setResponse(modelToUpdate, scope.textResponse);
            });

             scope.$watch('status', function (newValue, oldValue) {
                    if (newValue == 'SUBMITTED') {
                        // status has changed to submitted
                        var correctResponse = scope.itemSession.sessionData.correctResponses[responseIdentifier];
                        var responseValue = "";
                        try {
                            var response =
                                QtiUtils.getResponseById(responseIdentifier, scope.itemSession.responses);// localScope.itemSession.responses[responseIdentifier].value;
                            if (response) {
                                responseValue = response.value;
                            }

                        } catch (e) {
                            // just means it isn't set, leave it as ""
                        }
                        var isSelected = QtiUtils.compare(scope.value, responseValue);
                        if (scope.isFeedbackEnabled() != false) {
                            // give the current choice the correct-response class if it is the correct response
                            if (QtiUtils.compare(scope.value, correctResponse)) {
                                element.toggleClass('correct-response');
                            }

                            if (isSelected && ( QtiUtils.compare(scope.value, correctResponse) )) {
                                // user selected the right response
                                element.toggleClass('correct-selection');
                            } else if (isSelected) {
                                // user selected the wrong response
                                element.toggleClass('incorrect-selection');
                            }
                        }

                    }
                });

        }

    }
});