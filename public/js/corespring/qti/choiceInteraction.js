/**
 *  Process QTI 2.1 choiceInteraction element
 *  NOTE: we do not intend to support every feature of QTI
 */

qtiDirectives.directive("choiceinteraction", function(InlineFeedback) {

    // TODO: This should probably use Angular $routeParams, but I couldn't figure out the DI
    var itemId = window.location.href.match(/item\/(.*)/)[1];

    /**
     * TODO: This is pretty terrible. I'm sure Angular has some way to do this stuff, but I wanted to get something
     * together quickly to test the backend.
     */
    var array = {
        indexOf: function(a, obj) {
            for (var i = 0; i < a.length; i++) {
                if (a[i] === obj) {
                    return i;
                }
            }
            return -1;
        },
        contains: function(a, obj) {
            return array.indexOf(a, obj) >= 0;
        },
        remove: function(a, obj) {
            var index = array.indexOf(a, obj);
            if (index >= 0) {
                var rest = a.slice(index + 1 || a.length);
                a.length = index < 0 ? a.length + index : index;
                a.push.apply(a, rest);
            }
        }
    };

    /**
     * This function will locate a <feedbackInline> element with a specified csFeedbackId value. If no <feedbackInline>
     * element exists with the provided csFeedbackId, the function will return the first <feedbackInline> element found
     * on the page.
     */
    function getFeedbackByCsId(csFeedbackId) {
        var feedbackInlineElements = document.getElementsByTagName('feedbackInline');
        var modalFeedbackElements = document.getElementsByTagName("modalFeedback");

        for (var i = 0, j = feedbackInlineElements.length + modalFeedbackElements.length; i < j; i++) {
            var element = (i < feedbackInlineElements.length) ? feedbackInlineElements[i] :
                modalFeedbackElements[i - feedbackInlineElements.length];

            if (element.getAttribute('csFeedbackId') == csFeedbackId) {
                return element;
            }
        }
        return null;
    }

    // the html to use for an individual choice
    var choiceTemplate =
        '<span ng-bind-html-unsafe="prompt" class="choicePrompt"></span>' +
        '<div ng:repeat="choice in choices" class="simpleChoice"> ' +
             '<input type="{{inputType}}" name="simpleChoice" ng-click="click(choice.identifier)" ng-model="choice" value="{{choice.identifier}}"><span ng-bind-html-unsafe="choice.content"></span></input>' +
         '</div>';

    return {
        restrict: 'E',
        scope: true,
        compile: function(tElement, tAttrs, transclude) {
            // compile function is where DOM modification can happen
            // before we modify DOM with the html choiceTemplate, extract the data we need...

            // get the prompt element if present
            // it supports embedded html
            var result = tElement.find("prompt");
            var prompt = "";
            if (result.length == 1) {
               var promptElem = angular.element(result[0]);
               prompt = promptElem.html();
            }

            // get the simpleChoice elements
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

            // linking function, is the return value from compile function
            return function (scope, element, attrs, controller) {
                console.log('in link function');

                // read some stuff from attrs
                scope.responseidentifier = attrs.responseidentifier;
                scope.shuffle = attrs.shuffle;
                scope.maxchoices = attrs.maxchoices;

                // TODO - need to actually implement orientation feature, probably will just put this as a css class.
                // orientation = 'horizontal'|'vertical' ... Default to vertical
                scope.orientation = attrs.orientation;

                // is this a single-response or multi-response item?
                if (scope.maxchoices == 1) {
                    scope.inputType = 'radio';
                } else {
                    scope.inputType = 'checkbox';
                    scope.selectedchoices = [];
                }

                // populate the choices
                scope.choices = choices;

                // populate prompt
                scope.prompt = prompt;

                var feedback = {
                    update: function(element, choiceId, feedback) {
                        for (var i in feedback) {
                            var feedbackToUpdate = getFeedbackByCsId(feedback[i].csFeedbackId);
                            if (feedbackToUpdate) {
                                var newFeedback = document.createElement("div");
                                newFeedback.innerHTML = feedback[i].body;
                                feedbackToUpdate.parentNode.appendChild(newFeedback.firstChild);
                                feedbackToUpdate.parentNode.removeChild(feedbackToUpdate);
                            }
                        }

                    },
                    clear: function() {
                        var feedbackInlineElements = document.getElementsByTagName('feedbackInline');
                        var modalFeedbackElements = document.getElementsByTagName("modalFeedback");

                        for (var i = 0, j = feedbackInlineElements.length + modalFeedbackElements.length; i < j; i++) {
                            var element = (i < feedbackInlineElements.length) ? feedbackInlineElements[i] :
                                modalFeedbackElements[i - feedbackInlineElements.length];

                            element.innerHTML = '';
                        }
                    }
                };

                // called when this choice is clicked
                scope.click = function(choiceId) {
                    feedback.clear();
                    console.log("input clicked");
                    var multiple = scope.maxchoices != 1;

                    if (multiple) {
                        array.contains(scope.selectedchoices, choiceId) ?
                            array.remove(scope.selectedchoices, choiceId) : scope.selectedchoices.push(choiceId);
                    }
                    var response = InlineFeedback.get({
                        itemId: itemId,
                        responseIdentifier: attrs.responseidentifier,
                        identifier: multiple ? scope.selectedchoices : choiceId
                    }, function() {
                        feedback.update(element, choiceId, response.feedback);
                    });
                }
            }
        }
    }
});


