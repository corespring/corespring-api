angular.module('tagger')
  .directive('itemEditButtons', [function () {

    function link($scope, $element, $attrs, ngModel){
      $scope.status = 'draft';

      $scope.$watch('item.published', function(isPublished){

        $scope.status = isPublished ? 'live' : 'draft';

      });
    }

    return {
      restrict: 'AE',
      link: link,
      replace: true,
      scope: {
        item: '=',
        editItem: '&',
        viewItem: '&',
        goLive: '&',
        makeADraft: '&',
        cloneItem: '&'
      }, 
      template: [
      '<div>',
      '  <div class="item-status" ng-switch on="status">',
      '    <div class="item-status-holder" ng-switch-when="live">',
      '      <button ng-click="makeADraft(item)" class="btn btn-sm">Make a draft</button>',
      '    </div>',
      '    <div class="item-status-holder" ng-switch-when="draft">',
      '      <button ng-click="editItem(item)" ng-show="!item.readOnly" class="btn btn-sm" aria-label="edit">',
      '        <i class="icon icon-pencil" aria-hidden="true"></i>',
      '        Edit the Draft',
      '      </button>',
      '      <button ng-click="viewItem(item)" ng-show="item.readOnly" class="btn btn-sm" aria-label="edit">',
      '        <i class="icon icon-eye-open" aria-hidden="true"></i>',
      '        View',
      '      </button>',
      '      <br/>',
      '      <button ng-click="goLive(item)" ng-show="!item.readOnly" class="btn btn-sm">Go live</button>',
      '    </div>',
      '  </div>',
      '  <button ng-click="cloneItem(item)" class="btn btn-sm">Clone</button>',
      '</div>'].join('')
    };
  }]);
