angular.module('tagger')
  .directive('itemStatus', [function () {

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
      '    <span class="label label-primary">LIVE</span>',
      '    <span class="text">{{sessions}} Sessions(s)</span>',
      '  </div>',
      '  <div class="item-status-holder" ng-switch-when="draft">',
      '    <span class="label label-default">DRAFT</span>',
      '    <span class="text">{{revisions}} Revisions(s)</span>',
      '  </div>',
      '</div>'].join('')
    };
  }]);
