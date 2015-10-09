angular.module('tagger.directives').directive('toastMessage', [
  '$timeout',
  function($timeout) {

    function link($scope, $element, $attr) {

      var eventToListenFor = $attr.toastEvent;
      var timeout = parseInt($attr.toastTimeout, 10) || 3000;

      $scope.showToast = false;
      $scope.$on(eventToListenFor, function(event) {
        if ($scope.showToast === false) {
          $scope.showToast = true;
          $timeout(function() {
            $scope.showToast = false;
          }, timeout);
        }
      });
    }

    return {
      restrict: 'C',
      link: link,
      transclude: true,
      template: '<span ng-class="{hidden: !showToast}" ng-transclude></span>',
      scope: true
    };
  }]);