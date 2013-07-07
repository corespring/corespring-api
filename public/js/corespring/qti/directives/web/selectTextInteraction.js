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
    template: "<span ng-click='toggle()' ng-transclude></span>",
    transclude: true,
    require: "^selecttextinteraction",
    compile: function compile(tElement, tAttrs) {
      var outerHtml = tElement[0].outerHTML;

      return function link(scope, iElement, iAttrs) {
        iAttrs.$set("enabled", "true");
        scope.isSelected = false;
        scope.id = /id=[^0-9]*([0-9]+)/gim.exec(outerHtml)[1];

        function switchClass(className) {
          return function (newVal, oldVal) {
            if (newVal == oldVal) return;

            if (newVal && !$(iElement).children("span").hasClass(className)) {
              $(iElement).children("span").switchClass("", className, 200);
            }
            else if (!newVal && $(iElement).children("span").hasClass(className)) {
              $(iElement).children("span").switchClass(className, "", 200);
            }
          }
        }

        scope.$watch("isSelected", switchClass("selected"));
        scope.$watch("shouldHaveBeenSelected", switchClass("shouldHaveBeenSelected"));
        scope.$watch("shouldNotHaveBeenSelected", switchClass("shouldNotHaveBeenSelected"));
        scope.$watch("unknown", switchClass("unknown"));

        var mouseOver = function () {
          if (scope.disabled) return;
          $(iElement).children("span").switchClass("", "hover", 200);
          $(iElement).children("span").addClass("hand");
        }
        var mouseOut = function () {
          if (scope.disabled) return;
          $(iElement).children("span").switchClass("hover", "", 200);
          $(iElement).children("span").removeClass("hand");
        }
        $(iElement).mouseover(mouseOver);
        $(iElement).mouseleave(mouseOut);
        $(iElement).mouseout(mouseOut);

        scope.toggle = function () {
          if (scope.disabled) return;
          scope.isSelected = !scope.isSelected;
          if (scope.isSelected)
            scope.addSelection(scope.id);
          else
            scope.removeSelection(scope.id);
        };

        var highlightCorrectAndIncorrect = function() {
          if (!scope.itemSession || !scope.itemSession.sessionData || !scope.itemSession.sessionData.correctResponses) return;
          var correctResponse = QtiUtils.getResponseValue(scope.responseIdentifier, scope.itemSession.sessionData.correctResponses, "");
          var givenResponse = QtiUtils.getResponseValue(scope.responseIdentifier, scope.itemSession.responses, "");
          var response = QtiUtils.getResponseById(scope.responseIdentifier, scope.itemSession.responses);
          var outcome = response ? response.outcome : {};
          scope.isSelected = (givenResponse.indexOf(scope.id) >= 0);
          if (scope.itemSession.sessionData.correctResponses.length == 0) {
            scope.unknown = true;
          } else {
            scope.shouldHaveBeenSelected = !scope.onlyCountMatch && correctResponse.indexOf(scope.id) >= 0;
            scope.shouldNotHaveBeenSelected = !scope.onlyCountMatch && (scope.isSelected && correctResponse.indexOf(scope.id) < 0);
            if (scope.onlyCountMatch && scope.isSelected) {
              scope.shouldHaveBeenSelected = outcome.responsesCorrect;
              scope.shouldNotHaveBeenSelected = !outcome.responsesCorrect;
            }
          }
        }

        // Not final submission - highlight only whether the given answers are correct or not
        scope.$watch('itemSession.sessionData', function () {
          highlightCorrectAndIncorrect();
          scope.shouldHaveBeenSelected &= scope.isSelected;
        });

        // Final submission - highlight the correct answers as well even if they did not get chosen
        scope.$on('formSubmitted', function () {
          highlightCorrectAndIncorrect();
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