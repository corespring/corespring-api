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

            /**
             * Note - in choiceInteraction.compile we wrap this in varying number of divs - so we need to find the choiceInteraction ancestor node.
             */
            var choiceInteractionElem = tElement.parent();
            var i = 0;
            while (choiceInteractionElem.attr('responseidentifier') == undefined) {
                choiceInteractionElem = choiceInteractionElem.parent();
                if (i++ > 10) throw new Error("Parent choice interaction not found");
            }
            var maxChoices = choiceInteractionElem.attr('maxChoices');
            var isHorizontal = choiceInteractionElem.attr('orientation') == 'horizontal';
            var inputType = maxChoices == 1 ? 'radio' : 'checkbox';


            var nodeWithFeedbackRemoved = tElement.html().replace(feedbackInlineRegex, "");

            var responseIdentifier = choiceInteractionElem.attr('responseidentifier');

            var divs = isHorizontal ? [
                    '<div class="simple-choice-inner-horizontal" ng-class="{noResponse: noResponse}">',
                    '   <div class="choice-content-horizontal" ng-class="{noResponse: noResponse}"> ' + nodeWithFeedbackRemoved + '</div>',
                    '   <div ng-class="{noResponse: noResponse}"><input type="' + inputType + '" ng-click="onClick()" ng-disabled="formDisabled" ng-model="chosenItem" value="{{value}}"></input></div>',
                    '</div>']

                    :

                    ['<div class="simple-choice-inner">',
                    '  <div class="choiceInput">',
                    '    <input type="' + inputType + '" ng-click="onClick()" ng-disabled="formDisabled" ng-model="chosenItem" value="{{value}}"></input></div>',
                    '  <div class="choice-content"> ' + nodeWithFeedbackRemoved + '</div>',
                    '</div>',
                    createFeedbackContainerDiv(tElement.html())]

                ;

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

                var applyCss = function (correct, selected) {
                    tidyUp();
                    if (selected) {
                        var className = correct ? 'correct-selection' : 'incorrect-selection';
                        element.toggleClass(isHorizontal ? (className+"-horizontal") : className);
                    }
                    if (correct) {
                        element.toggleClass(isHorizontal ? 'correct-response-horizontal' : 'correct-response');
                    }
                };

                var tidyUp = function() {
                    element
                        .removeClass('correct-selection')
                        .removeClass('incorrect-selection')
                        .removeClass('correct-response')
                        .removeClass('correct-selection-horizontal')
                        .removeClass('incorrect-selection-horizontal')
                        .removeClass('correct-response-horizontal');
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
                    applyCss(isCorrect, isSelected());
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
        var isHorizontal = attrs["orientation"] === "horizontal";
        var html = element.html();
        var finalContents = shuffle ? getShuffledContents(html) : html;

        var newNode = isHorizontal ?
            ('<div ng-class="{noResponse: noResponse}"><div class="choice-interaction">' + finalContents + '</div><div style="clear: both"></div></div>')
            :
            ('<div class="choice-interaction" ng-class="{noResponse: noResponse}">' + finalContents + '</div>')
        element.html(newNode);
        return link;
    };


    var link = function (scope, element, attrs, AssessmentItemCtrl, $timeout) {

        scope.controller = AssessmentItemCtrl;

        var maxChoices = attrs['maxchoices'];
        var modelToUpdate = attrs["responseidentifier"];

        var mode = maxChoices == 1 ? "radio" : "checkbox";

        scope.controller.setResponse(modelToUpdate, undefined);

        scope.$watch('showNoResponseFeedback', function(newVal, oldVal) {
            scope.noResponse = (scope.isEmptyItem(scope.chosenItem) && scope.showNoResponseFeedback);
        });

        var toggleChosenItem = function(value){
            scope.chosenItem = (scope.chosenItem || []);

            if (scope.chosenItem.indexOf(value) == -1) {
                scope.chosenItem.push(value);
            } else {
                var idx = scope.chosenItem.indexOf(value);
                if (idx != -1) scope.chosenItem.splice(idx, 1);
            }
        };

        scope.setChosenItem = function (value) {
            if (mode == "checkbox") {
                toggleChosenItem(value);
                scope.controller.setResponse(modelToUpdate, scope.chosenItem);
            } else {
                scope.chosenItem = value;
                scope.controller.setResponse(modelToUpdate, value);
            }
            scope.noResponse = (scope.isEmptyItem(scope.chosenItem) && scope.showNoResponseFeedback);
        };
    };

    /**
     * NOTE: We disable replace and transclude.
     * We are going to do this manually to support shuffling.
     */
    return {
        restrict:'E',
        scope: true,
        require:'^assessmentitem',
        compile: compile,
        controller:function ($scope) {
            this.scope = $scope;
        }
    }
});
