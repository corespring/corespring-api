angular.module('customColors.directives').directive('choiceIcon', function() {
  return {
    restrict: 'E',
    scope: {
      'key': '@',
      'shape': '@'
    },
    template: [
      '<span class="{{key}}">',
      '  <span>',
      '    <div style="width: 30px;" class="choice-icon">',
      '      <ng-include src="template"/>',
      '    </div>',
      '  </span>',
      '</span>'
    ].join('\n'),
    link: function($scope, $element, $attrs) {
      var pn = window.location.pathname;
      var firstPathSegment = pn.substring(0, pn.indexOf('/',1));
      $scope.template = firstPathSegment+'/v2/player/images/components-assets/choice/' + [$scope.shape, $scope.key].join('-') + '.svg';
      $scope.$watch('key+shape', function() {
        $scope.template = firstPathSegment+'/v2/player/images/components-assets/choice/' + [$scope.shape, $scope.key].join('-') + '.svg';
      });
    }
  }
});