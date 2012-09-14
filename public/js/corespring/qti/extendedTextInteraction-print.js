/**
 * handles QTI 2.1 extendedTextInteraction which is intended for text area text responses
 */
qtiDirectives.directive("extendedtextinteraction", function() {
    return {
        restrict: 'E',
        replace: true,
        template: '<div class="extendedTextInteraction"></div>',
        compile: function(element, attrs, transclude){
            var newElement = element;
            //var newElement = angular.element('<div class="extendedTextInteraction"></div>') ;

            // create a div for each line
            var numLines = 4;
            if (attrs.expectedLines) {
                numLines = attrs.expectedlines;
            }
            for (i=0; i<=numLines; i++) {
                newElement.append('<div class="extendedTextLine"/>')
            }

            element.html(newElement.html());

        }

    }
});
