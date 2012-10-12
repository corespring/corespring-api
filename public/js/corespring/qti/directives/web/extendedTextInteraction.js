/**
 * handles QTI 2.1 extendedTextInteraction which is intended for text area text responses
 */
qtiDirectives.directive("extendedtextinteraction", function() {
    return {
        restrict: 'E',
        replace: true,
        scope: true,
        require: '^assessmentitem',
        template: '<div ng-class="{noResponse: noResponse}"><textarea rows="{{rows}}" cols="{{cols}}" ng-model="extResponse" ng-disabled="formDisabled"></textarea></div>',
        link: function (scope, element, attrs, AssessmentItemController) {

            scope.rows = 4; // default # of rows
            scope.cols = 60; // default # of cols

            if (attrs.expectedLines) {
                scope.rows = attrs.expectelines;
            }

            // read some stuff from attrs
            var modelToUpdate = attrs.responseidentifier;
            scope.expectedLength = attrs.expectedlength;
            scope.maxStrings = attrs.maxstrings;
            scope.minStrings = attrs.minstrings;

            scope.$watch('showNoResponseFeedback', function (newVal, oldVal) {
                scope.noResponse = (scope.isEmptyItem(scope.extResponse) && scope.showNoResponseFeedback);
            });

            scope.$watch('extResponse', function(newVal, oldVal) {
                AssessmentItemController.setResponse(modelToUpdate, scope.extResponse);
                scope.noResponse = (scope.isEmptyItem(scope.extResponse) && scope.showNoResponseFeedback);
            });

        }

    }
});
