angular.module('tagger')
  .directive('itemStatus', [function () {

    function link($scope, $element, $attrs){
      $scope.status = 'draft';

      $scope.$watch('item.published', function(isPublished){
        $scope.status = isPublished ? 'live' : 'draft';

        if($scope.status === 'live'){
          $scope.sessions = $scope.getNumberOfSessions($scope.id);
        }
      });

      $scope.$watch('item.id', function(id){
        var split = id.split(':');
        $scope.revisions = split.length === 2 ? split[1] : 'No' ;
      });
    }

    return {
      restrict: 'AE',
      link: link,
      replace: true,
      scope: {
        item: '=',
        getNumberOfSessions: '&'
      },
      template: [
      '<div class="item-status" ng-switch on="status">',
      '  <div class="item-status-holder" ng-switch-when="live">',
      '    <span class="label label-primary">LIVE</span>',
      '    <br/>',
      '    <span class="text">{{sessions}} Sessions(s)</span>',
      '  </div>',
      '  <div class="item-status-holder" ng-switch-when="draft">',
      '    <span class="label label-default">DRAFT</span>',
      '    <br/>',
      '    <span class="text">{{revisions}} Revisions(s)</span>',
      '  </div>',
      '</div>'].join('')
    };
  }]);
