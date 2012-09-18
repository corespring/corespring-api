
/**
 * Interaction for ordering a set of choices
 */

qtiDirectives.directive('orderinteraction', function () {




    var choiceTemplate = [
        '<span ng-bind-html-unsafe="prompt" class="choice-prompt"></span>',
        '<div sortable="" class="sortable-body">',
        '<div ng:repeat="item in items" obj="{{item}}"> ',
        '<span class="sortable-item {{item.submittedClass}}" ng-bind-html-unsafe="item.content"></span>',
        ' </div>',
        '</div>'].join('');


    return {
        restrict: 'E',
        scope: true,
        require: '^assessmentitem',
        compile: function(tElement, tAttrs, transclude) {
            // compile function

            // get the prompt element if present
            // it supports embedded html
            var result = tElement.find("prompt");
            var prompt = "";
            if (result.length == 1) {
                var promptElem = angular.element(result[0]);
                prompt = promptElem.html();
            }

            // get the simple-choice elements
            // they support embedded html
            var choices = [];
            console.log('compile function');
            var choiceElements = angular.element(tElement).find("simpleChoice");
            for (var i = 0; i < choiceElements.length; i++)  {
                var elem = angular.element(choiceElements[i]);
                var identifier = elem.attr('identifier');
                choices.push({content: elem.html(), identifier: identifier});
            }

            // now modify the DOM
            tElement.html(choiceTemplate);



            // linking function
            return function(scope, element, attrs, AssessmentItemCtrl) {


                var responseIdentifier = attrs["responseidentifier"];
                // set model to choices extracted from html
                scope.orderedList = [];
                scope.result = [];
                scope.prompt = prompt;
                scope.items = choices;

                // watch the response and set it to the responses list
                scope.$watch('orderedList', function (newValue, oldValue) {
                    var flattenedArray = [];
                    for (var i = 0; i < newValue.length; i++) {
                        flattenedArray[i] = newValue[i].identifier;
                    }

                    AssessmentItemCtrl.setResponse(responseIdentifier, flattenedArray);
                });

                // handle updating this view after submission
                scope.$watch('status', function(newValue, oldValue) {
                    if (newValue == 'SUBMITTED') {
                        // TODO disable further interaction with the widget now that it is submitted

                        // break if feedback is not enabled
                        if (scope.isFeedbackEnabled() == false) return;

                        // initialize the item css feedback to incorrect
                        for (var y = 0; y < scope.items.length; y++) {
                            scope.items[y].submittedClass = "orderIncorrect";
                        }
                        // get the correct response
                        var correctResponse = scope.sessionData.correctResponse[responseIdentifier];
                        var response = scope.itemSession.responses[responseIdentifier];
                        // for each item, determine if item is in right or wrong place
                        for (var i = 0; i < correctResponse.length; i++) {
                            if (correctResponse[i] == response.value[i]) {
                                // this response is in the right place
                                // find the item with the current identifier and set css to correct
                                for (var x = 0; x < scope.items.length; x++) {
                                    if (scope.items[x].identifier == response.value[i]) {
                                        scope.items[x].submittedClass = "orderCorrect";
                                    }
                                }

                            }

                        }
                    }
                });

            };  // end linking function
        }

    }
});


qtiDirectives.directive("sortable", function() {
    return {
        // todo look into isolate scope so orderedList is not on global scope, tried it but was having trouble
        link: function(scope, el, attrs, ctrl) {

            var startParent = null;
            var stopParent = null;
            $(el).sortable({
                items: 'div:not(:has(div.complete))',
                start: function(event, ui) {
                    startParent = $(ui.item).parent();
                },
                stop: function(event, ui) {
                    stopParent = $(ui.item).parent();
                    //var stopData = scope.list;
                    var items = [];
                    stopParent.children('div').each(function(index) {
                        var liItem = scope.$eval($(this).attr('obj'));
                        liItem.ord = index;
                        items.push(liItem);
                    });
                    scope.orderedList  = items;
                    //scope.$apply();

                },
                connectWith: '.sortable-linked',
                placeholder: 'ui-state-highlight',
                forcePlaceholderSize: true,
                axis: 'y',
                dropOnEmpty: true
            });
        }
    }
});