angular.module('qti.directives').directive("textentryinteraction", function (QtiUtils) {


  function positionTooltip(a, currentElement) {
    return "bottom";
  }

  return {
    restrict: 'E',
    replace: true,
    scope: true,
    require: '^assessmentitem',
    template: ['<span class="text-entry-interaction" ng-class="{noResponse: noResponse}">',
               '<input type="text" size="{{expectedLength}}" ng-model="textResponse" ng-disabled="formSubmitted"></input>'
               ].join(""),
    link: function (scope, element, attrs) {
      var responseIdentifier = attrs.responseidentifier;

      scope.$watch('aggregate', function (aggregate) {
        if (!aggregate) return;
        var agg = aggregate[responseIdentifier];
        if (!agg) return;

        scope.textResponse = agg.correctAnswers[0];
        scope.choices = agg.choices;
        var totalCorrect = agg.numCorrectResponses;
        var total = agg.totalResponses;
        var pCorrect = (totalCorrect * 100 / total).toFixed(0);
        var pIncorrect  = (100 - (totalCorrect * 100 / total)).toFixed(0);
        var tooltip = "<table class='tooltiptable'>";
        tooltip += "<tr><th colspan='3'>";
        tooltip +=  "Overall";
        tooltip += "<tr><th colspan='3'>";
        tooltip +=  pCorrect+'% Correct / '+pIncorrect+'% Incorrect <br/>';
        tooltip += "<tr><td colspan='3'>Top 5 incorrect Responses";
        tooltip += "<tr><td>Submitted Response<td>Frequency";
        var sorted = [];
        for (var c in agg.choices) {
          if (agg.correctAnswers.indexOf(c) < 0)
            sorted.push([c, agg.choices[c]]);
        }
        sorted.sort(function(a, b) {return b[1] - a[1]});

        for (var i = 0; i < Math.min(sorted.length, 5); i++) {
          tooltip += "<tr><td>"+sorted[i][0]+"</td><td>"+sorted[i][1]+"</td></tr>";
        }

        $(element).tooltip({ title: tooltip, placement: positionTooltip(this) });
      });

    }
  }
});