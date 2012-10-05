qtiDirectives.directive('simplechoice', function (QtiUtils) {

    return {
        restrict:'E',
        replace:true,
        scope:true,
        require:'^choiceinteraction',

        /**
         * We manually build our template so that we can move the feedbackinline
         * nodes out of the content node and into their own now. This
         * makes it easier to style them.
         * @param tElement
         * @param tAttrs
         * @param transclude
         * @return {Function}
         */
        compile:function (tElement, tAttrs, transclude) {

            var feedbackInlineRegex = /(<feedbackinline.*?>.*?<\/feedbackinline>)/g;

            /**
             * Build a div with <feedbackinline> nodes from the incoming html.
             * @param html
             * @return {String}
             */
            var createFeedbackContainerDiv = function (html) {
                var feedbackNodes = html.match(feedbackInlineRegex);

                if (!feedbackNodes) {
                    return "";
                }

                var feedbackContainer = "<div class='feedback-container'>";
                for (var i = 0; i < feedbackNodes.length; i++) {
                    feedbackContainer += feedbackNodes[i];
                }
                feedbackContainer += "</div>";
                return feedbackContainer;
            };

            // determine input type by inspecting markup before modifying DOM
            var inputType = 'checkbox';
            var choiceInteractionElem = tElement.parent();
            var maxChoices = choiceInteractionElem.attr('maxChoices');

            if (maxChoices == 1) {
                inputType = 'radio';
            }


            var nodeWithFeedbackRemoved = tElement.html().replace(feedbackInlineRegex, "");

            var responseIdentifier = choiceInteractionElem.attr('responseidentifier');

            var divs = ['<div class="simple-choice-inner">',
                '  <div class="choiceInput">',
                '    <input type="' + inputType + '" ng-click="onClick()" ng-disabled="formDisabled" ng-model="chosenItem" value="{{value}}"></input></div>',
                '  <div class="choice-content"> ' + nodeWithFeedbackRemoved + '</div>',
                '</div>',
                createFeedbackContainerDiv(tElement.html())];

            var template = divs.join("\n");

            // now can modify DOM
            tElement.html(template);

            // return link function
            return function (localScope, element, attrs, choiceInteractionController) {
                localScope.disabled = false;

                localScope.value = attrs.identifier;

                localScope.controller = choiceInteractionController;
                localScope.$watch('controller.scope.chosenItem', function (newValue, oldValue) {
                    // todo - don't like this special case, but need it to update ui. Look into alternative solution
                    if (inputType == 'radio') {
                        localScope.chosenItem = newValue;
                    }
                });

                localScope.onClick = function () {
                    choiceInteractionController.scope.setChosenItem(localScope.value);
                };

                var isSelected = function () {
                    var responseId = QtiUtils.getResponseValue(responseIdentifier, localScope.itemSession.responses, "");
                    return QtiUtils.compare(localScope.value, responseId);
                };

                var isOurResponseCorrect = function (correctResponse) {
                    return QtiUtils.compare(localScope.value, correctResponse)
                };

                var applyCss = function (correct) {
                    var className = correct ? 'correct-selection' : 'incorrect-selection';
                    element.toggleClass(className);
                };


                var tidyUp = function() {
                    element
                        .removeClass('correct-selection')
                        .removeClass('incorrect-selection')
                        .removeClass('correct-response');
                };


                localScope.$on( 'resetUI', function( event ){
                    tidyUp();
                });


                // watch the status of the item, update the css if this is the chosen response
                // and if it is correct or not
                localScope.$watch('itemSession.sessionData.correctResponses', function (responses) {

                    if (!responses) return;
                    if (!localScope.isFeedbackEnabled()) return;

                    var correctResponse = responses[responseIdentifier];
                    var isCorrect = isOurResponseCorrect(correctResponse);

                    if (isCorrect) {
                        element.toggleClass('correct-response');
                    }

                    if (!isSelected()) return;

                    applyCss(isCorrect);
                });

            };
        }

    };
});

qtiDirectives.directive('choiceinteraction', function () {



    var simpleChoiceRegex = /(<simplechoice[\s\S]*?>[\s\S]*?<\/simplechoice>)/gm;

    /**
     * @param html
     * @return {String}
     */
    var getSimpleChoicesArray = function (html) {
        var nodes = html.match(simpleChoiceRegex);

        if (!nodes) {
            return [];
        }

        return nodes;
    };

    var isFixedNode = function( node ){
       return node.indexOf("fixed='true'") != -1 || node.indexOf('fixed="true"') != -1;
    };

    var getFixedIndexes = function( simpleChoiceNodes ){

        var out = [];
        for( var i  = 0 ; i < simpleChoiceNodes.length ; i++){
            var node = simpleChoiceNodes[i];

            if( isFixedNode(node)){
                out.push(i);
            }
        }
        return out;
    };

    /**
     * Get the simplechoice nodes - shuffle those that need shuffling,
     * then add the back to the original html
     * @param html
     * @return {*}
     */
    var getShuffledContents = function( html ) {

        var TOKEN = "__SHUFFLED_CHOICES__";
        var simpleChoicesArray = getSimpleChoicesArray(html);
        var fixedIndexes = getFixedIndexes(simpleChoicesArray);

        var contentsWithChoicesStripped =
            html.replace( /<simplechoice[\s\S]*?>[\s\S]*<\/simplechoice>/gm, TOKEN);

        var shuffled = simpleChoicesArray.shuffle(fixedIndexes);
        return contentsWithChoicesStripped.replace(TOKEN, shuffled.join("\n") );
    };

    /**
     * shuffle the nodes if shuffle="true"
     */
    var compile = function(element, attrs, transclude){
        var shuffle = attrs["shuffle"] === "true";
        var html = element.html();
        var finalContents = shuffle ? getShuffledContents(html) : html;
        element.html( finalContents );
        return link;
    };


    var link = function (scope, element, attrs, AssessmentItemCtrl, $timeout) {

        var maxChoices = attrs['maxchoices'];
        // the model for an interaction is specified by the responseIdentifier
        var modelToUpdate = attrs["responseidentifier"];

        // TODO need to handle shuffle and fixed options... probably need to rearrange the DOM in compile function for this

        AssessmentItemCtrl.setResponse(modelToUpdate, undefined);

        scope.$watch('showNoResponseFeedback', function(newVal, oldVal) {
            scope.noResponse = (scope.isEmptyItem(scope.chosenItem) && scope.showNoResponseFeedback);
        });

        scope.setChosenItem = function (value) {
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
                    if (idx != -1) scope.chosenItem.splice(idx, 1); // Remove it if really found!
                }
                AssessmentItemCtrl.setResponse(modelToUpdate, scope.chosenItem);
            } else {
                scope.chosenItem = value;
                AssessmentItemCtrl.setResponse(modelToUpdate, value);
            }
            scope.noResponse = (scope.isEmptyItem(scope.chosenItem) && scope.showNoResponseFeedback);
        };
    };

    return {
        restrict:'E',
        replace:true,
        scope:true,
        require:'^assessmentitem',
        compile: compile,
        controller:function ($scope) {
            this.scope = $scope;
        }
    }
});
