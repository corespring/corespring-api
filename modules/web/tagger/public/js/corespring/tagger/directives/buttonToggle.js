// module for using css-toggle buttons instead of checkboxes
// toggles the class named in button-toggle element if value is checked
angular.module('buttonToggle', []).directive('buttonToggle', function () {
    return {
        restrict:'A',
        require:'ngModel',
        link:function ($scope, element, attr, ctrl) {
            var classToToggle = attr.buttonToggle;
            element.bind('click', function () {
                var checked = ctrl.$viewValue;
                $scope.$apply(function (scope) {
                    ctrl.$setViewValue(!checked);
                });
            });

            $scope.$watch(attr.ngModel, function (newValue, oldValue) {
                newValue ? element.addClass(classToToggle) : element.removeClass(classToToggle);
            });
        }
    };
});

