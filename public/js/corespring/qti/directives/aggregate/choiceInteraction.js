qtiDirectives.directive('choiceinteraction', function () {

  var simpleChoiceRegex = /(<:*simplechoice[\s\S]*?>[\s\S]*?<\/:*simplechoice>)/gmi;

  var getSimpleChoicesArray = function (html) {
    var nodes = html.match(simpleChoiceRegex);
    if (!nodes) {
      return [];
    }
    return nodes;
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

    var contentsWithChoicesStripped =
      html.replace(/<:*simplechoice[\s\S]*?>[\s\S]*<\/:*simplechoice>/gmi, TOKEN);

    var choices = simpleChoicesArray;
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

    var statistics = ["<div>Completion Rate: {{percentageCompleted}}% ({{totalResponses}} responses, {{totalAssigned}} assignments)</div>",
      "<table><tr><th>Total Number</th><th>% Correct (students)</th><th>% Incorrect (students)</th><th>% Skipped (students)</th></tr>",
      "<tr><td>{{totalResponses}}</td><td>{{percentageCorrect}}% ({{totalCorrect}})</td><td>{{percentageIncorrect}}% ({{totalIncorrect}})</td><td>{{percentageSkipped}}% ({{totalSkipped}})</td></tr>",
      "</table>"
    ].join("");

    var newNode = (isHorizontal ?
      ('<div ng-class="{noResponse: noResponse}"><div class="choice-interaction">' + prompt + statistics + '<div class="choice-wrap">' + finalContents + '</div></div><div style="clear: both"></div></div>')
      :
      ('<div class="choice-interaction" ng-class="{noResponse: noResponse}">' + prompt + statistics + finalContents + '</div>')
    );
    element.html(newNode);
    return link;
  };


  var link = function (scope, element, attrs, AssessmentItemCtrl, $timeout) {


    var maxChoices = attrs['maxchoices'];
    var mode = maxChoices == 1 ? "radio" : "checkbox";
    var responseIdentifier = attrs['responseidentifier'];

    scope.$watch('aggregate', function (aggregate) {
      if (!aggregate) return;
      var agg = aggregate[responseIdentifier];
      if (!agg) return;

      var total = scope.totalResponses = agg.totalResponses;
      scope.totalAssigned = scope.totalResponses;
      scope.percentageCompleted = (total * 100 / scope.totalAssigned).toFixed(0);
      scope.totalCorrect = agg.numCorrectResponses;
      scope.percentageCorrect = (scope.totalCorrect * 100 / total).toFixed(0)
      scope.totalIncorrect = scope.totalResponses - scope.totalCorrect;
      scope.percentageIncorrect = (scope.totalIncorrect * 100 / total).toFixed(0)
      scope.totalSkipped = scope.totalAssigned - total;
      scope.percentageSkipped = (scope.totalSkipped * 100 / total).toFixed(0)
    });


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
