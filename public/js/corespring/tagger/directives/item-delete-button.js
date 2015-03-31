angular.module('tagger')
  .directive('itemDeleteButton', [function () {

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

      function isNotReadOnlyAndTheresNoDrafts(){
        if($scope.item.readOnly){
          return false;
        } else {
          return $scope.draftStatus === 'noDraft'  || $scope.draftStatus === 'canMakeDraft';
        }
      }

      function updateFlags(){
        updateDraftStatus();
        $scope.canDelete = !$scope.item.readOnly && $scope.draftStatus !== 'draftExists';
        $scope.deleteLabel = $scope.draftStatus === 'ownsDraft' ? 'Delete Draft' : 'Delete Item';
      }

      $scope['delete'] = function(){
        if($scope.draftStatus === 'ownsDraft'){
          $scope.$eval($scope.deleteDraft)($scope.draft);
        } else {
          $scope.deleteItem($scope.item);
        }
      };

      $scope.$watch('userName', updateFlags);

      $scope.$watch('orgDrafts', function(drafts){
        $scope.draft = _.find(drafts, function(d){
          return d.itemId == $scope.item.id;
        });
        updateFlags();
      });

      $scope.$watch('item', updateFlags);

    }

    return {
      restrict: 'AE',
      link: link,
      replace: true,
      scope: {
        userName: '=',
        org: '=',
        orgDrafts: '=',
        item: '=',
        deleteItem: '&',
        //Because 'draft isnt in the isolated scope, we need to parse this ourselves'
        deleteDraft: '@'
      }, 
      template: [
      '<div>',
      '  <button ng-show="canDelete" ng-click="delete()" class="btn btn-danger">{{deleteLabel}}</button>',
      '</div>'].join('')
    };
  }]);
