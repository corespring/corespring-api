angular.module('qti.directives').directive('selecttextinteraction', function factory() {
  return {
    restrict: 'E',
    scope: true,
    require: '^assessmentitem',
    controller: function ($scope) {
      $scope.selections = [];
      $scope.addSelection = function (selection) {
        $scope.selections.push(selection);
      };
      $scope.removeSelection = function (selection) {
        var newSelections = [];
        for (var i = 0; i < $scope.selections.length; i++)
          if ($scope.selections[i] != selection) newSelections.push($scope.selections[i]);
        $scope.selections = newSelections;
      };
    },
    link: function link(scope, element, attrs, AssessmentItemController) {
      scope.controller = AssessmentItemController;
      scope.onlyCountMatch = attrs.checkifcorrect != "yes";
      var responseIdentifier = attrs.responseidentifier;
      scope.responseIdentifier = responseIdentifier;
      scope.$watch('selections.length', function () {
        scope.controller.setResponse(responseIdentifier, scope.selections);
        scope.noResponse = (scope.isEmptyItem(scope.selections) && scope.showNoResponseFeedback);
      });
      scope.$on('unsetSelection', function (event) {
        scope.selections = [];
      });

    }
  };
});

angular.module('qti.directives').directive('selectable', function factory(QtiUtils) {
  return {
    restrict: 'AC',
    scope: true,
    replace: false,
    template: "<span><span ng-transclude /><span class='{{numClass}}'>{{num}}</span></span>",
    transclude: true,
    require: "^selecttextinteraction",
    compile: function compile(tElement, tAttrs) {
      var outerHtml = tElement[0].outerHTML;

      return function link(scope, iElement, iAttrs) {
        iAttrs.$set("enabled", "true");
        scope.isSelected = false;
        scope.id = /id=[^0-9]*([0-9]+)/gim.exec(outerHtml)[1];

        scope.$watch('aggregate', function (aggregate) {
          if (!aggregate) return;
          var agg = aggregate[scope.responseIdentifier];
          scope.num = agg.choices[scope.id];
          var isCorrect = agg.correctAnswers.indexOf(scope.id) >= 0;
          scope.numClass = isCorrect ? "correct" : "incorrect";
        });


        // Final submission - highlight the correct answers as well even if they did not get chosen
        scope.$on('formSubmitted', function () {
          scope.disabled = true;
          iAttrs.$set("enabled", "false");
        });

        scope.$on('unsetSelection', function (event) {
          scope.disabled = false;
          scope.isSelected = false;
          scope.shouldHaveBeenSelected = false;
          scope.shouldNotHaveBeenSelected = false;
          iAttrs.$set("enabled", "true");
        });
      }
    }
  };
});