angular.module('tagger.directives')
  .directive('itemActionButton', [function () {

    function link($scope, $element, $attrs, ngModel){
      $scope.status = 'draft';
      $scope.draftStatus = 'noDraft';

      $scope.$watch('item.published', function(isPublished){
        $scope.status = isPublished ? 'live' : 'draft';
      });

      function updateDraftStatus(){

        if(!$scope.item.readOnly){
          $scope.draftStatus = 'canMakeDraft';
        }

        if($scope.draft && !$scope.item.readOnly){
          if($scope.org && 
            $scope.draft.orgId === $scope.org.id && 
            $scope.draft.user === $scope.userName){
            $scope.draftStatus = 'ownsDraft';
          } else {
            $scope.draftStatus = 'draftExists';
          }
        }
      }
      
      function isReadOnly(){
        return $scope.item && $scope.item.readOnly;
      }

      function isPublished(){
        return $scope.item && $scope.item.published;
      }

      function hasDraft(){
        return $scope.draftStatus !== 'noDraft' && $scope.draftStatus !== 'canMakeDraft';
      }

      function updateFlags(){
        updateDraftStatus();

        $scope.canGoLive = !isReadOnly() && (!isPublished() || hasDraft());
        $scope.canClone = !$scope.item.readOnly; 


        switch ($scope.draftStatus) {
          case 'ownsDraft' : 
            $scope.canDelete = true;
            $scope.deleteLabel = 'Delete Draft';
            break;
          case 'draftExists' : 
            $scope.canDelete = false;
            break;
          case 'canMakeDraft' : 
            $scope.canDelete = true;
            $scope.deleteLabel = 'Delete';
        }
      }

      $scope.$watch('userName', updateFlags);

      $scope.$watch('orgDrafts', function(drafts){
        $scope.draft = _.find(drafts, function(d){
          return d.itemId == $scope.item.id;
        });
        updateFlags();
      });

      $scope.$watch('org', function(o){
        if(o){
          updateFlags();
        }
      });

      $scope.$watch('item', updateFlags, true);

      $scope['delete'] = function(){
        if($scope.draftStatus === 'ownsDraft'){
          $scope.$eval($scope.deleteDraft)($scope.draft);
        } else {
          $scope.deleteItem($scope.item);
        }
      };

    }

    return {
      restrict: 'AE',
      link: link,
      replace: true,
      scope: {
        userName: '=',
        orgDrafts: '=',
        org: '=',
        item: '=',
        publish: '&',
        cloneItem: '&',
        deleteItem: '&',
        //Because 'draft isnt in the isolated scope, we need to parse this ourselves'
        deleteDraft: '@'

      }, 
      template: [
      '<div>',
      '<a class="btn btn-default btn-sm dropdown-toggle" data-toggle="dropdown" href="#menu-item-{{item.id}}">actions <b class="caret"></b></a>',
      '<div class="dropdown" id="menu-item-{{item.id}}">',
        '<ul class="dropdown-menu dropdown-menu-actions">',
          '<li>',
          '  <div class="draft-buttons" ng-switch on="draftStatus">',
          '    <div ng-switch-when="draftExists">',
          '      <i class="icon icon-lock"></i>',
          '      <span>{{draft.user}} is editing</span>',
          '    </div>',
          '    <div ng-switch-default>',
          '      <button class="btn btn-sm" ng-click="cloneItem(item)" ng-disabled="item.readOnly" ng-show"canClone"><i class="fa fa-plus-circle"></i>&nbsp;clone</button>',
          '    </div>',
          '  </div>',
          '</li>',
          '<div ng-show="canGoLive"><li><div><button class="btn btn-sm btn-info" ng-click="publish(item)" ng-show="canGoLive"><i class="fa fa-bolt"></i>&nbsp;publish</button></div></li></div>',
          '<li><button ng-show="canDelete" ng-click="delete()" class="btn btn-danger btn-sm"><i class="fa fa-trash-o"></i>&nbsp;{{deleteLabel}}</button></li>',
        '</ul>',
      '</div>',
      '</div>'].join('')
    };
  }]);
