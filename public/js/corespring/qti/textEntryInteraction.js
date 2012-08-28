/**
 * handles QTI 2.1 textEntryInteraction which is intended for in-line text reponses
 */
qtiDirectives.directive("textentryinteraction", function() {


    return {
        restrict: 'E',
        replace: true,
        scope: true,
        template: '<span><input type="text" size="{{expectedLength}}" ng-model="response"></input></span>',
        link: function (scope, element, attrs, controller) {
                console.log('in link function');

                // read some stuff from attrs
                scope.responseidentifier = attrs.responseidentifier;
                scope.expectedLength = attrs.expectedlength;

                // called when this choice is clicked
                scope.$watch('response', function(oldVal, newVal) {
                    console.log('response: ' + scope.response);
                });

            }

    }
});


