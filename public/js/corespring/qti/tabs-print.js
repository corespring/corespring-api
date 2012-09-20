qtiDirectives.directive('csTabs', function() {
      return {
        restrict: 'E',
        transclude: true,
        controller: function($scope, $element, $attrs) {},
        template: '<div class="tab-panel"><div ng-transclude /></div>'

      };
    }
  );

qtiDirectives.directive('csTab', function() {
      return {
        restrict: 'E',
        transclude: true,
        require: '^csTabs',
        link: function(scope, elm, attrs, container) {
                scope.title = attrs['title'];
              },
        scope: true,
        template: "<div class=\"tab-title\">{{title}}</div><div class=\"tab-box\" ng-transclude></div>"
      };
    }
);
