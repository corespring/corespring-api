angular.module('tagger')
  .directive('itemStatus', [function () {

    function link($scope, $element, $attrs){
      $scope.status = '?';

      $scope.$watch('item.published', function(isPublished){
        $scope.status = isPublished ? 'live' : 'draft';
     });
    }

    return {
      restrict: 'AE',
      link: link,
      replace: true,
      scope: {
        item: '='
      },
      template: [
      '<div class="item-status" ng-switch on="status">',
      '  <div class="item-status-holder" ng-switch-when="live">',
      '    <span class="label label-info"><i class="fa fa-bolt"></i></span>',
      '  </div>',
      '</div>'].join('')
    };
  }]);
