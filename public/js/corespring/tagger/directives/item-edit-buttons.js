angular.module('tagger')
  .directive('itemEditButtons', [function () {

    function link($scope, $element, $attrs, ngModel){
      $scope.status = 'draft';
    }

    return {
      restrict: 'AE',
      require: '^ngModel',
      link: link,
      replace: true,
      scope: 'isolate',
      template: [
      '<div class="item-status" ng-switch on="status">',
      '  <div class="item-status-holder" ng-switch-when="live">',
      '    <button class="btn btn-sm">Make a draft</button>',
      '  </div>',
      '  <div class="item-status-holder" ng-switch-when="draft">',
      '    <button class="btn btn-sm">Go live</button>',
      '  </div>',
      '  <button class="btn btn-sm">Clone</button>',
      '</div>'].join('')
    };
  }]);
