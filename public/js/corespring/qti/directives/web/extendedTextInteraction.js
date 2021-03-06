/**
 * handles QTI 2.1 extendedTextInteraction which is intended for text area text responses
 */
angular.module('qti.directives').directive("extendedtextinteraction", function (QtiUtils) {
  return {
    restrict: 'E',
    replace: true,
    scope: true,
    require: '^assessmentitem',
    template: '<div ng-class="{noResponse: noResponse}"><textarea rows="{{rows}}" cols="{{cols}}" ng-model="extResponse" ng-disabled="formSubmitted"></textarea></div>',
    link: function (scope, element, attrs, AssessmentItemController) {

      scope.controller = AssessmentItemController;
      scope.rows = attrs.expectedlines || 4; // default # of rows
      scope.cols = attrs.expectedlength || attrs.cols || 60; // default # of cols

      // read some stuff from attrs
      var modelToUpdate = attrs.responseidentifier;
      scope.maxStrings = attrs.maxstrings;
      scope.minStrings = attrs.minstrings;

      scope.$watch('showNoResponseFeedback', function () {
        scope.noResponse = (scope.isEmptyItem(scope.extResponse) && scope.showNoResponseFeedback);
      });

      scope.$on('unsetSelection', function () {
        scope.extResponse = "";
      });

      scope.$watch('extResponse', function () {
        AssessmentItemController.setResponse(modelToUpdate, scope.extResponse);
        scope.noResponse = (scope.isEmptyItem(scope.extResponse) && scope.showNoResponseFeedback);
      });

      scope.$on('highlightUserResponses', function () {
        scope.extResponse = QtiUtils.getResponseValue(modelToUpdate, scope.itemSession.responses);
      });

    }

  };
});
