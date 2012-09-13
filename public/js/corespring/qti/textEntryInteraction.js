qtiDirectives.directive("textentryinteraction", function() {


    return {
        restrict: 'E',
        replace: true,
        scope: true,
        require: '^assessmentitem',
        template: '<span><input type="text" size="{{expectedLength}}" ng-model="textResponse" ng-disabled="formDisabled"></input></span>',
        link: function (scope, element, attrs, AssessmentItemController) {
            // read some stuff from attrs
            var modelToUpdate = attrs.responseidentifier;
            scope.expectedLength = attrs.expectedlength;

            // called when this choice is clicked
            scope.$watch('textResponse', function(newVal, oldVal) {
                AssessmentItemController.setResponse(modelToUpdate, scope.textResponse);
            });

        }

    }
});