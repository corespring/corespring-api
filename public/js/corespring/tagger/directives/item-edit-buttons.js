angular.module('tagger')
  .directive('itemEditButton', [function () {

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
          if($scope.org && $scope.draft.orgId === $scope.org.id){
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

      $scope.edit = function(){

        if(!$scope.item || !$scope.item.format ){
          return;
        } 

        if($scope.item.readOnly){
          return;
        }

        if($scope.item.format.apiVersion === 1){
          $scope.editItem($scope.item);
        } else if($scope.item.format.apiVersion === 2){
          $scope.editDraft($scope.item);
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
        editItem: '&',
        editDraft: '&',
        goLive: '&',
        makeADraft: '&',
        cloneItem: '&'
      }, 
      template: [
      '<div>',
      '  <div>Format: {{item.format.apiVersion}}</div>',
      '  <div class="draft-buttons" ng-switch on="draftStatus">',
      '    <div ng-switch-when="draftExists">',
      '      <i class="icon icon-lock"></i>',
      '      <span>Draft owner: {{draft.orgId}}</span>',
      '    </div>',
      '    <button ng-disabled="item.readOnly" ng-click="edit()" class="btn btn-sm">Edit</button>',
      '    <button ng-click="goLive(item)" ng-show="canGoLive" class="btn btn-sm">Go live</button>',
      '  </div>',
      '  <button ng-click="cloneItem(item)" ng-show"canClone" class="btn btn-sm">Clone</button>',
      '</div>'].join('')
    };
  }]);
