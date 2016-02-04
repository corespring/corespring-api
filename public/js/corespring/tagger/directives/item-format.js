angular.module('tagger.directives')
  .directive('itemFormat', ['$log', function ($log) {
    
    var template = '<div>{{format}}</div>';

    return {
      restrict: 'AE',
      replace: true,
      template: template,
      scope: {
        format: '='
      }
    };

  }]);
