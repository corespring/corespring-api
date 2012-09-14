qtiDirectives.directive("textentryinteraction", function() {


    return {
        restrict: 'E',
        replace: true,
        scope: true,
        require: '^assessmentitem',
        template: '<span><div class="textEntryInteraction" style="width: {{expectedLength}}em"></input></span>',
        link: function (scope, element, attrs, AssessmentItemController) {

            scope.expectedLength = attrs.expectedlength;


        }

    }
});