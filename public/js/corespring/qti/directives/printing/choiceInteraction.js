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

    var newNode = ('<div class="choice-interaction" ng-class="{noResponse: noResponse}">' + prompt + finalContents + '</div>');
    element.html(newNode);
    return function(){};
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
