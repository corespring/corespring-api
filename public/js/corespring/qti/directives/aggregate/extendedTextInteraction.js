qtiDirectives.directive("extendedtextinteraction", function (QtiUtils) {
  return {
    restrict: 'E',
    replace: true,
    scope: true,
    require: '^assessmentitem',
    template: '<div ng-class="{noResponse: noResponse}"><textarea rows="{{rows}}" cols="{{cols}}" ng-model="extResponse" ng-disabled="formSubmitted"></textarea></div>',
    link: function (scope, element, attrs, AssessmentItemController) {

      $(element).tooltip({ title: "Open Response" });

      scope.controller = AssessmentItemController;
      scope.rows = 4; // default # of rows
      scope.cols = 60; // default # of cols

      if (attrs.expectedlines) {
        scope.rows = attrs.expectedlines;
      }

    }

  };
});
