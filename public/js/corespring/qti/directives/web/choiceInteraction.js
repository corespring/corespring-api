angular.module('qti.directives').directive('choiceinteraction', function () {

  var simpleChoiceRegex = /(<:*simplechoice[\s\S]*?>[\s\S]*?<\/:*simplechoice>)/gmi;

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

  var insertAlphabetPrefixes = function (choices) {
    for (var i = 0; i < choices.length; i++) {
      var ch = '';
      var times = Math.floor(i / 32);
      for (var j = 0; j < times + 1; j++)
        ch += String.fromCharCode(65 + i % 32);

      choices[i] = choices[i].replace(/(<:*simplechoice[\s\S]*?>)/, "$1" + ch + ". &nbsp;");
    }
  }

  /**
   * Get the simplechoice nodes - shuffle those that need shuffling,
   * then add the back to the original html
   */
  var getContents = function (html, shuffle, insertLetters) {

    var TOKEN = "__SHUFFLED_CHOICES__";
    var simpleChoicesArray = getSimpleChoicesArray(html);
    var fixedIndexes = getFixedIndexes(simpleChoicesArray);


    var contentsWithChoicesStripped =
      html.replace(/<:*simplechoice[\s\S]*?>[\s\S]*<\/:*simplechoice>/gmi, TOKEN);

    var choices = shuffle ? simpleChoicesArray.shuffle(fixedIndexes) : simpleChoicesArray;
    if (insertLetters) insertAlphabetPrefixes(choices);

    return contentsWithChoicesStripped.replace(TOKEN, choices.join("\n"));
  };

  /**
   * shuffle the nodes if shuffle="true"
   */
  var compile = function (element, attrs, transclude) {

    var shuffle = attrs["shuffle"] === "true";
    var insertLetters = !(attrs["insertletters"] == "false");
    var isHorizontal = attrs["orientation"] === "horizontal";
    var html = element.html();

    var promptMatch = html.match(/<:*prompt>((.|[\r\n])*?)<\/:*prompt>/);
    var prompt = "<span class=\"prompt\">" + ((promptMatch && promptMatch.length > 0) ? promptMatch[1] : "") + "</span>";

    // We convert custom elements to attributes in order to support IE8
    var finalContents = getContents(html, shuffle, insertLetters)
      .replace(/<:*prompt>(.|[\r\n])*?<\/:*prompt>/gim, "")
      .replace(/<:*simpleChoice/gi, "<span simplechoice").replace(/<\/:*simpleChoice>/gi, "</span>")
      .replace(/<:*feedbackInline/gi, "<span feedbackinline").replace(/<\/:*feedbackInline>/gi, "</span>");

    var newNode = isHorizontal ?
      ('<div ng-class="{noResponse: noResponse}"><div class="choice-interaction">' + prompt + '<div class="choice-wrap">' + finalContents + '</div></div><div style="clear: both"></div></div>')
      :
      ('<div class="choice-interaction" ng-class="{noResponse: noResponse}">' + prompt + finalContents + '</div>')
    element.html(newNode);
    return link;
  };


  var link = function (scope, element, attrs, AssessmentItemCtrl, $timeout) {

    scope.controller = AssessmentItemCtrl;

    scope.controller.registerInteraction(element.attr('responseIdentifier'), element.find('.prompt').html(), "choice");

    var maxChoices = attrs['maxchoices'];
    var modelToUpdate = attrs["responseidentifier"];

    var mode = maxChoices == 1 ? "radio" : "checkbox";


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

      if (isChosen === undefined) {
        throw "You have to specify 'isChosen' either true/false";
      }

      if (mode == "checkbox") {
        scope.chosenItem = (scope.chosenItem || []);
        if (isChosen) {
          setCheckboxChoice(value);
        } else {
          unsetCheckboxChoice(value);
        }
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
    restrict: 'E',
    scope: true,
    require: '^assessmentitem',
    compile: compile,
    controller: function ($scope) {
      this.scope = $scope;
    }
  }
});
