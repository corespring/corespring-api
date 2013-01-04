qtiDirectives.directive('focustaskinteraction', function () {

    var choiceRegex = /(<:*focusChoice[\s\S]*?>[\s\S]*?<\/:*focusChoice>)/gmi;

    var getSimpleChoicesArray = function (html) {
        var nodes = html.match(choiceRegex);
        if (!nodes) {
            return [];
        }
        return nodes;
    };

    var isFixedNode = function (node) {
        return node.indexOf("fixed='true'") != -1 || node.indexOf('fixed="true"') != -1;
    };

    var getFixedIndexes = function (simpleChoiceNodes) {
        var out = [];
        for (var i = 0; i < simpleChoiceNodes.length; i++) {
            var node = simpleChoiceNodes[i];

            if (isFixedNode(node)) {
                out.push(i);
            }
        }
        return out;
    };


    var insertRowSeparatos = function (choices) {
        var result = ['<div class="focus-row">'];
        for (var i = 0; i < choices.length; i++) {
            result.push(choices[i]);
            if (i % 5 == 4)
                result.push('</div><div class="focus-row">');
        }
        result.push("</div>");

        return result;
    };

    var getContents = function (html, shuffle) {
        var TOKEN = "__SHUFFLED_CHOICES__";
        var choicesArray = getSimpleChoicesArray(html);

        var resultArray = insertRowSeparatos(shuffle ? choicesArray.shuffle(getFixedIndexes(choicesArray)) : choicesArray);
        var contentsWithChoicesStripped = html.replace(/<:*focusChoice[\s\S]*?>[\s\S]*<\/:*focusChoice>/gmi, TOKEN);
        return contentsWithChoicesStripped.replace(TOKEN, resultArray.join("\n"));
    };

    var compile = function (element, attrs) {
        var shuffle = attrs["shuffle"] === "true";
        var html = element.html();
        var promptMatch = html.match(/<:*prompt>((.|[\r\n])*?)<\/:*prompt>/);
        var prompt = "<span class=\"prompt\">" + ((promptMatch && promptMatch.length > 0) ? promptMatch[1] : "") + "</span>";

        var finalContents = getContents(html, shuffle)
            .replace(/<:*prompt>(.|[\r\n])*?<\/:*prompt>/gim, "")
            .replace(/<:*focusChoice/gi, "<span focuschoice").replace(/<\/:*focusChoice>/gi, "</span>");

        var newNode =
            ('<div class="choice-interaction" ng-class="{noResponse: noResponse}">' + prompt + '<div class="focus-container">' + finalContents + '</div></div>');

        element.html(newNode);
        return link;
    };


    var link = function (scope, element, attrs, AssessmentItemCtrl, $timeout) {

        scope.controller = AssessmentItemCtrl;
        scope.controller.registerInteraction(element.attr('responseIdentifier'), element.find('.prompt').html(), "choice");
        var modelToUpdate = attrs["responseidentifier"];
        scope.responseIdentifier = modelToUpdate;
        scope.controller.setResponse(modelToUpdate, undefined);
        scope.onlyCountMatch = attrs.checkifcorrect != "yes";
        scope.itemShape = attrs.itemshape || "square";

        scope.$watch('showNoResponseFeedback', function (newVal, oldVal) {
            scope.noResponse = (scope.isEmptyItem(scope.chosenItem) && scope.showNoResponseFeedback);
        });

        var unsetCheckboxChoice = function (value) {
            var index = scope.chosenItem.indexOf(value);
            if (index != -1) scope.chosenItem.splice(index, 1);
        };

        var setCheckboxChoice = function (value) {
            if (scope.chosenItem.indexOf(value) == -1) {
                scope.chosenItem.push(value);
            }
        };

        scope.$on('unsetSelection', function (event) {
            scope.chosenItem = [];
        });

        scope.setChosenItem = function (value, isChosen) {
            if (isChosen === undefined) {
                throw "You have to specify 'isChosen' either true/false";
            }

            scope.chosenItem = (scope.chosenItem || []);
            if (isChosen) {
                setCheckboxChoice(value);
            } else {
                unsetCheckboxChoice(value);
            }
            scope.controller.setResponse(modelToUpdate, scope.chosenItem);
            scope.noResponse = (scope.isEmptyItem(scope.chosenItem) && scope.showNoResponseFeedback);
        };
    };

    return {
        restrict: 'E',
        scope: true,
        require: '^assessmentitem',
        compile: compile,
        controller: function ($scope) {
            this.scope = $scope;
        }
    }
});


qtiDirectives.directive('focuschoice', function (QtiUtils) {

    var linkFn = function (scope, iElement, attrs, focusTaskInteractionController) {
        scope.selected = false;
        scope.unknown = false;
        scope.controller = focusTaskInteractionController;

        scope.click = function () {
            if (scope.disabled) return;
            scope.setChosenItem(scope.value, !scope.selected);
            scope.unknown = scope.shouldHaveBeenSelected = scope.shouldNotHaveBeenSelected = false;
        };
        scope.disabled = false;
        scope.value = attrs.identifier;

        scope.$watch('itemShape', function (newValue) {
            scope.square = (newValue == 'square');
            scope.circle = !scope.square;
        });

        scope.$watch('chosenItem.length', function (newValue) {
            if (!scope.chosenItem) return;
            scope.selected = scope.chosenItem.indexOf(scope.value) != -1;
        });

        scope.$on('highlightUserResponses', function (event) {
            var givenResponse = QtiUtils.getResponseValue(scope.responseIdentifier, scope.itemSession.responses, "");
            scope.selected = (givenResponse.indexOf(scope.value) >= 0);
        });

        scope.$watch('itemSession.sessionData.correctResponses', function (responses) {
            if (!responses) return;
            if (!scope.itemSession || !scope.itemSession.sessionData || !scope.itemSession.sessionData.correctResponses) return;
            var correctResponse = QtiUtils.getResponseValue(scope.responseIdentifier, scope.itemSession.sessionData.correctResponses, "");
            console.log(correctResponse);
            var response = QtiUtils.getResponseById(scope.responseIdentifier, scope.itemSession.responses);
            var withinLimits = response.outcome && !response.outcome.responsesBelowMin && !response.outcome.responsesExceedMax;
            if (responses.length == 0) {
                scope.unknown = true;
            }
            else if (withinLimits) {
                var isCorrect = correctResponse.indexOf(scope.value) >= 0;
                console.log(scope.highlightUserResponse() , scope.selected , !scope.onlyCountMatch , isCorrect);
                scope.shouldHaveBeenSelected = scope.highlightUserResponse() && scope.selected && !scope.onlyCountMatch && isCorrect;
                scope.shouldHaveBeenSelected |= !scope.onlyCountMatch && scope.highlightCorrectResponse() && isCorrect ;
                scope.shouldHaveBeenSelected |= scope.onlyCountMatch && scope.selected;
                scope.shouldNotHaveBeenSelected = !scope.onlyCountMatch && scope.highlightUserResponse() && !scope.onlyCountMatch && scope.selected && !isCorrect;
            } else {
                scope.shouldHaveBeenSelected = scope.shouldNotHaveBeenSelected = false;
            }
        });

        scope.$watch('formSubmitted', function (newValue) {
            scope.disabled = newValue;
        });

        scope.$on('resetUI', function (event) {
            scope.shouldHaveBeenSelected = false;
            scope.shouldNotHaveBeenSelected = false;
        });

        scope.$on('unsetSelection', function (event) {
            scope.selected = false;
            scope.disabled = false;
            scope.shouldHaveBeenSelected = false;
            scope.shouldNotHaveBeenSelected = false;
            attrs.$set("enabled", "true");
        });
    };

    return {
        restrict: 'ACE',
        require: '^focustaskinteraction',
        replace: true,
        scope: true,
        template: "<div class='focus-element' ng-class='{unknown: unknown, square: square, circle: circle, selected: selected, shouldHaveBeenSelected: shouldHaveBeenSelected, shouldNotHaveBeenSelected: shouldNotHaveBeenSelected}' ng-click='click()' ng-transclude></div>",
        transclude: true,
        link: linkFn

    }
});