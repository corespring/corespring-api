qtiDirectives.directive('focustaskinteraction', function () {

    var simpleChoiceRegex = /(<:*focusChoice[\s\S]*?>[\s\S]*?<\/:*focusChoice>)/gmi;

    var getSimpleChoicesArray = function (html) {
        var nodes = html.match(simpleChoiceRegex);

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

    var getShuffledContents = function (html) {

        var TOKEN = "__SHUFFLED_CHOICES__";
        var simpleChoicesArray = getSimpleChoicesArray(html);
        var fixedIndexes = getFixedIndexes(simpleChoicesArray);


        var contentsWithChoicesStripped =
            html.replace(/<:*focusChoice[\s\S]*?>[\s\S]*<\/:*focusChoice>/gmi, TOKEN);


        var shuffled = simpleChoicesArray.shuffle(fixedIndexes);


        return contentsWithChoicesStripped.replace(TOKEN, shuffled.join("\n"));
    };

    var compile = function (element, attrs) {

        var shuffle = attrs["shuffle"] === "true";
        var html = element.html();

        var promptMatch = html.match(/<:*prompt>((.|[\r\n])*?)<\/:*prompt>/);
        var prompt = "<span class=\"prompt\">" + ((promptMatch && promptMatch.length > 0) ? promptMatch[1] : "") + "</span>";

        // We convert custom elements to attributes in order to support IE8
        var finalContents = (shuffle ? getShuffledContents(html) : html)
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
        scope.controller.setResponse(modelToUpdate, undefined);
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
            console.log("[Parent] Setting ",value," to ",isChosen);
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

    var linkFn = function (scope, element, attrs, focusTaskInteractionController) {
        scope.selected = false;
        scope.controller = focusTaskInteractionController;
        scope.click = function () {
            console.log("Setting ",scope.value," to ",!scope.selected);
            scope.setChosenItem(scope.value, !scope.selected);
        };
        scope.disabled = false;
        scope.value = attrs.identifier;

        scope.$watch('chosenItem.length', function (newValue) {
            console.log("Chosen item chgd", newValue);
            if (!scope.chosenItem) return;
            scope.selected = scope.chosenItem.indexOf(scope.value) != -1;
        });

        var isSelected = function () {
            var responseValue = QtiUtils.getResponseValue(responseIdentifier, scope.responses, "");
            return QtiUtils.compare(scope.value, responseValue);
        };

    };

    return {
        restrict: 'ACE',
        require: '^focustaskinteraction',
        replace: true,
        scope: true,
        template: "<div class='focus-element' ng-class='{selected: selected}' ng-click='click()'><span class='inner' ng-transclude /></div>",
        transclude: true,
        link: linkFn

    }
});