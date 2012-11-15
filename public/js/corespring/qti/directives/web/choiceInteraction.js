qtiDirectives.directive('simplechoice', function (QtiUtils) {

    return {
        restrict:'ACE',
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

            var feedbackInlineRegex = /(<span.*?feedbackInline.*?<\/.*?>)/gi;

            /**
             * Build a div with <feedbackinline> nodes from the incoming html.
             * @param html
             * @return {String}
             */
            var createFeedbackContainerDiv = function (html, returnContainerIfEmpty) {
                var feedbackNodes = html.match(feedbackInlineRegex);


                var feedbackContainer = "<div class='feedback-container'>";
                if (!feedbackNodes) {
                    return returnContainerIfEmpty ? feedbackContainer+"</div>" : "";
                }


                for (var i = 0; i < feedbackNodes.length; i++) {
                    feedbackContainer += feedbackNodes[i];
                }
                feedbackContainer += "</div>";

                console.log(feedbackContainer);
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
                    '   <div ng-class="{noResponse: noResponse}"><input type="' + inputType + '" ng-click="onClick()" ng-disabled="formSubmitted" ng-model="chosenItem" value="{{value}}"></input></div>',
                    '</div>',
                    createFeedbackContainerDiv(tElement.html(), true)
                    ]

                    :

                    ['<div class="simple-choice-inner">',
                    '  <div class="choiceInput">',
                    '    <input type="' + inputType + '" ng-click="onClick()" ng-disabled="formSubmitted" ng-model="chosenItem" value="{{value}}"></input></div>',
                    '  <div class="choice-content"> ' + nodeWithFeedbackRemoved + '</div>',
                    '</div>',
                    createFeedbackContainerDiv(tElement.html())
                    ]

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
                    var responseId = QtiUtils.getResponseValue(responseIdentifier, localScope.responses, "");
                    return QtiUtils.compare(localScope.value, responseId);
                };

                var isOurResponseCorrect = function (correctResponse) {
                    return QtiUtils.compare(localScope.value, correctResponse)
                };

                var applyCorrectResponseStyle = function(){
                    clear('correct-response', 'correct-response-horizontal');

                    element.toggleClass(isHorizontal ? 'correct-response-horizontal' : 'correct-response');
                };

                var clear = function(){
                    for(var i = 0 ; i < arguments.length; i++){
                        element.removeClass(arguments[i]);
                    }
                };

                var tidyUp = function() {
                    element
                        .removeClass('incorrect-response')
                        .removeClass('correct-response')
                        .removeClass('incorrect-response-horizontal')
                        .removeClass('correct-response-horizontal');
                };


                localScope.$on( 'resetUI', function( event ){
                    tidyUp();
                });

                localScope.$on( 'unsetSelection', function(event){
                    localScope.chosenItem = [];
                });


                // watch the status of the item, update the css if this is the chosen response
                // and if it is correct or not
                localScope.$watch('itemSession.sessionData.correctResponses', function (responses) {

                    if (!responses) return;

                    var correctResponse = QtiUtils.getResponseValue(responseIdentifier, responses, "");
                    var isCorrect = isOurResponseCorrect(correctResponse);

                    tidyUp();

                    if(isCorrect && localScope.highlightCorrectResponse() ){
                        applyCorrectResponseStyle();
                    }
                    if (isSelected() && localScope.highlightUserResponse()) {
                        var className = isCorrect ? 'correct-response' : 'incorrect-response';
                        element.addClass(isHorizontal ? (className+"-horizontal") : className);
                    }
                });

            };
        }

    };
});

qtiDirectives.directive('choiceinteraction', function () {



    var simpleChoiceRegex = /(<:*simplechoice[\s\S]*?>[\s\S]*?<\/:*simplechoice>)/gmi;

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
            html.replace( /<:*simplechoice[\s\S]*?>[\s\S]*<\/:*simplechoice>/gmi, TOKEN);


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

        var promptMatch = html.match(/<:*prompt>(.*?)<\/:*prompt>/);
        var prompt = "<span class=\"prompt\">"+((promptMatch && promptMatch.length > 0) ? promptMatch[1] : "")+"</span>";

        // We convert custom elements to attributes in order to support IE8
        var finalContents = (shuffle ? getShuffledContents(html) : html)
            .replace(/<:*prompt>.*?<\/:*prompt>/gi, "")
            .replace(/<:*simpleChoice/gi, "<span simplechoice").replace(/<\/:*simpleChoice>/gi, "</span>")
            .replace(/<:*feedbackInline/gi, "<span feedbackinline").replace(/<\/:*feedbackInline>/gi, "</span>");

        var newNode = isHorizontal ?
            ('<div ng-class="{noResponse: noResponse}"><div class="choice-interaction">'+prompt+'<div class="choice-wrap">' + finalContents + '</div></div><div style="clear: both"></div></div>')
            :
            ('<div class="choice-interaction" ng-class="{noResponse: noResponse}">' + prompt + finalContents + '</div>')
        element.html(newNode);
        return link;
    };


    var link = function (scope, element, attrs, AssessmentItemCtrl, $timeout) {

        scope.controller = AssessmentItemCtrl;

        scope.controller.registerInteraction( element.attr('responseIdentifier'), element.find('.prompt').html());

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

        scope.$on( 'unsetSelection', function(event){
            scope.chosenItem = [];
        });


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
