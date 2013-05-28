
/**
 * Interaction for ordering a set of choices
 */

angular.module('qti.directives').directive('orderinteraction', function () {




    var choiceTemplate = [
        '<span ng-bind-html-unsafe="prompt" class="choice-prompt"></span>',
        '<div ng:repeat="choice in choices"> ',
        '<div class="ordered-choice">',
        '<div class="ordered-choice-form"></div> <div class="ordered-choice-item" ng-bind-html-unsafe="choice.content"></div>',
        '</div>',
        ' </div>',
        '</div>'].join('');


    return {
        restrict: 'E',
        scope: true,
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

            var choiceElements = angular.element(tElement).find("simpleChoice");
            for (var i = 0; i < choiceElements.length; i++)  {
                var elem = angular.element(choiceElements[i]);
                var identifier = elem.attr('identifier');
                choices.push({content: elem.html(), identifier: identifier});
            }

            // now modify the DOM
            tElement.html(choiceTemplate);

            // linking function
            return function(scope, element, attrs) {

                scope.prompt = prompt;
                scope.choices = choices;

            };  // end linking function
        }

    }
});


