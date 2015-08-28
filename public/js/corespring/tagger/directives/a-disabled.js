angular.module('tagger.directives')
  .directive('aDisabled', function() {
    return {
      compile: function(element, attr, transclude) {
        attr.ngClick = "!(" + attr.aDisabled + ") && (" + attr.ngClick + ")";

        return function($scope, $elem, $attrs) {

          $scope.$watch($attrs.aDisabled, function(newValue) {
            if (newValue !== undefined) {
              $elem.toggleClass("disabled", newValue);
            }
          });

          $elem.on("click", function(e) {
            if ($scope.$eval($attrs.aDisabled)) {
              e.preventDefault();
            }
          });
        };
      }
    };
  });