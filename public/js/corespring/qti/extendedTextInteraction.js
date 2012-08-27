/**
 * handles QTI 2.1 extendedTextInteraction which is intended for text area text responses
 */
qtiDirectives.directive("extendedtextinteraction", function() {



    return {
        restrict: 'E',
        replace: true,
        scope: true,
        template: '<textarea rows="{{rows}}" cols="{{cols}}"></textarea>',
        link: function (scope, element, attrs, controller) {
            console.log('in link function');

            scope.rows = 4; // default # of rows
            scope.cols = 60; // default # of cols

            if (attrs.expectedLines) {
                scope.rows = attrs.expectelines;
            }

            // read some stuff from attrs
            scope.responseidentifier = attrs.responseidentifier;
            scope.expectedLength = attrs.expectedlength;
            scope.maxStrings = attrs.maxstrings;
            scope.minStrings = attrs.minstrings;

            // called when this choice is clicked
            scope.$watch('response', function(oldVal, newVal) {
                console.log('response: ' + scope.response);
            });

        }

    }
});
