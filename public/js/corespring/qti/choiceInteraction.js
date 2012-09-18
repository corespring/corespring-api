qtiDirectives.directive('simplechoice', function(QtiUtils){

    return {
        restrict: 'E',
        replace: true,
        scope: true,
        transclude: true,
        require: '^choiceinteraction',
        compile: function(tElement, tAttrs, transclude){
            // determine input type by inspecting markup before modifying DOM
            var inputType = 'checkbox';
            var choiceInteractionElem = tElement.parent();
            var maxChoices = choiceInteractionElem.attr('maxChoices');

            if (maxChoices == 1) {
                inputType = 'radio';
            }

            var responseIdentifier = choiceInteractionElem.attr('responseidentifier');


            var template =  '<div ng-transclude="true"><input type="' + inputType + '" ng-click="onClick()" ng-disabled="formDisabled" ng-model="chosenItem" value="{{value}}"></input></div>';

            // now can modify DOM
            tElement.html(template);

            // return link function
            return function(localScope, element, attrs, choiceInteractionController) {

                localScope.disabled = false;

                localScope.value = attrs.identifier;

                localScope.controller = choiceInteractionController;
                localScope.$watch('controller.scope.chosenItem', function(newValue, oldValue) {
                    // todo - don't like this special case, but need it to update ui. Look into alternative solution
                    if (inputType == 'radio') {
                        localScope.chosenItem = newValue;
                    }
                });

                localScope.onClick = function(){
                    choiceInteractionController.scope.setChosenItem( localScope.value );
                };

                // watch the status of the item, update the css if this is the chosen response
                // and if it is correct or not
                localScope.$watch('status', function(newValue, oldValue) {
                    if (newValue == 'SUBMITTED') {
                        // status has changed to submitted
                        var correctResponse = localScope.sessionData.correctResponse[responseIdentifier];
                        var responseValue = "";
                        try {
                            responseValue = localScope.itemSession.responses[responseIdentifier].value;
                        } catch(e) {
                            // just means it isn't set, leave it as ""
                        }
                        var isSelected = QtiUtils.compare(localScope.value, responseValue);
                        if (localScope.isFeedbackEnabled() != false) {
                            // give the current choice the correctResponse class if it is the correct response
                            if (QtiUtils.compare(localScope.value, correctResponse)) {
                                element.toggleClass('correctResponse');
                            }

                            if (isSelected && ( QtiUtils.compare(localScope.value, correctResponse) )) {
                                // user selected the right response
                                element.toggleClass('correctSelection');
                            } else if (isSelected) {
                                // user selected the wrong response
                                element.toggleClass('incorrectSelection');
                            }
                        }

                    }
                }) ;

            };
        }

    };
});

qtiDirectives.directive('choiceinteraction', function () {

    return {
        restrict: 'E',
        transclude: true,
        template: '<div class="choice-interaction" ng-transclude="true"></div>',
        replace: true,
        scope: true,
        require: '^assessmentitem',
        link: function(scope, element, attrs, AssessmentItemCtrl, $timeout) {
            var maxChoices = attrs['maxchoices'];
            // the model for an interaction is specified by the responseIdentifier
            var modelToUpdate = attrs["responseidentifier"];

            // TODO need to handle shuffle and fixed options... probably need to rearrange the DOM in compile function for this

            scope.setChosenItem = function(value){
                if (maxChoices != 1) {
                    // multi choice means array model
                    if (scope.chosenItem == undefined) {
                        scope.chosenItem = [];
                    }
                    // check if it's in the array
                    if (scope.chosenItem.indexOf(value) == -1) {
                        // if not, push it
                        scope.chosenItem.push(value);
                    } else {
                        // otherwise remove it
                        var idx = scope.chosenItem.indexOf(value); // Find the index
                        if(idx!=-1) scope.chosenItem.splice(idx, 1); // Remove it if really found!
                    }
                    AssessmentItemCtrl.setResponse(modelToUpdate, scope.chosenItem);
                } else {
                    scope.chosenItem = value;
                    AssessmentItemCtrl.setResponse(modelToUpdate, value);
                }
            };


        },
        controller: function($scope) {
            this.scope = $scope;
        }
    }
});
