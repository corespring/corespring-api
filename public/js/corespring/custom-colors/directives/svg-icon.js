angular.module('customColors.directives').directive('svgIcon', function() {
  return {
    restrict: 'E',
    scope: {
      'iconSet': '@',
      'shape': '@',
      'text': '@',
      'open': '@'
    },
    template: [
      '<span class="{{key}}" ng-click="toggle()" ng-if="template" todo="{{template}}"> ',
      '  <div class="cs-icon">',
      '    <ng-include src="template"/>',
      '  </div>',
      '</span>'
    ].join('\n'),
    link: function($scope, $element, $attrs) {

      $scope.updateTemplate = function() {
        $scope.key = $attrs.key + ($scope.text ? '-feedback' : '');
        if (_.isEmpty($attrs.key)) {
          $scope.template = undefined;
          return;
        }

        var category = $attrs.category || 'feedback';
        var pn = window.location.pathname;
        var firstPathSegment = pn.substring(0, pn.indexOf('/',1));

        if ($scope.key === 'empty') {
          $scope.template = firstPathSegment + '/v2/player/images/components-assets/' + category + '/empty.svg'
        } else {
          $scope.template = firstPathSegment + '/v2/player/images/components-assets/' + category + '/'
            + ($scope.iconSet ? $scope.iconSet + '-' : '')
            + $scope.key
            + ($scope.shape ? '-' + $scope.shape : '')
            + ($scope.open ? '-open' : '')
            + '.svg';
        }

      };

      $attrs.$observe('key', $scope.updateTemplate);
      $scope.$watch('iconSet', $scope.updateTemplate);
      $scope.$watch('shape', $scope.updateTemplate);
      $scope.$watch('text', $scope.updateTemplate);
      $scope.$watch('open', $scope.updateTemplate);
    }
  }
});