qtiDirectives.directive("textentryinteraction", function (QtiUtils) {


  return {
    restrict: 'E',
    replace: true,
    scope: true,
    require: '^assessmentitem',
    template: ['<span class="text-entry-interaction" ng-class="{noResponse: noResponse}">',
               '<input type="text" size="{{expectedLength}}" ng-model="textResponse" ng-disabled="formSubmitted"></input>',
               ].join(""),
    link: function (scope, element, attrs) {
      var responseIdentifier = attrs.responseidentifier;

      scope.$watch('aggregate', function (aggregate) {
        if (!aggregate) return;

        var agg = aggregate[responseIdentifier];
        scope.textResponse = agg.correctAnswers[0];
        scope.choices = agg.choices;
        console.log(scope.choices);
        var totalCorrect = agg.numCorrectResponses;
        var total = agg.totalResponses;
        var pCorrect = (totalCorrect * 100 / total).toFixed(0);
        var pIncorrect  = (100 - (totalCorrect * 100 / total)).toFixed(0);
        var tooltip = pCorrect+'% Correct, '+pIncorrect+'% Incorrect <br/>';
        tooltip += "<br/>Top 5 incorrect answers:<br/>";
        tooltip += "<table class='tooltiptable'>";
        var sorted = [];
        for (var c in agg.choices) {
          sorted.push([c, agg.choices[c]]);
        }
        sorted.sort(function(a, b) {return b[1] - a[1]});

        for (var i = 0; i < sorted.length; i++) {
          tooltip += "<tr><td>"+sorted[i][0]+"</td><td>"+sorted[i][1]+"</td></tr>";
        }
        $(element).tooltip({ title: tooltip });
      });

    }
  }
});