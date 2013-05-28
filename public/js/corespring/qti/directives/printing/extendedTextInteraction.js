/**
 * handles QTI 2.1 extendedTextInteraction which is intended for text area text responses
 */
angular.module('qti.directives').directive("extendedtextinteraction", function() {
    return {
        restrict: 'E',
        replace: true,
        template: '<div class="extended-text-interaction"></div>',
        compile: function(element, attrs, transclude){
            var newElement = element;
            //var newElement = angular.element('<div class="extended-text-interaction"></div>') ;

            // create a div for each line
            var numLines = 4;
            if (attrs.expectedLines) {
                numLines = attrs.expectedlines;
            }
            for (i=0; i<=numLines; i++) {
                newElement.append('<div class="extended-text-line"/>')
            }

            element.html(newElement.html());

        }

    }
});
