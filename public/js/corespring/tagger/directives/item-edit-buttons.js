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
          if($scope.draft.user === $scope.userName){
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
        $scope.canGoLive = isNotReadOnlyAndTheresNoDrafts();
        $scope.canClone = !$scope.item.readOnly; 
      }

      $scope.$watch('userName', updateFlags);

      $scope.$watch('orgDrafts', function(drafts){
        $scope.draft = _.find(drafts, function(d){
          return d.itemId == $scope.item.id;
        });
        updateFlags();
      });

      $scope.$watch('item', updateFlags);

      $scope.callEditDraft = function(){
        $scope.$eval($scope.editDraft)($scope.draft);
      };

    }

    return {
      restrict: 'AE',
      link: link,
      replace: true,
      scope: {
        userName: '=',
        orgDrafts: '=',
        item: '=',
        editItem: '&',
        editDraft: '@',
        goLive: '&',
        makeADraft: '&',
        cloneItem: '&'
      }, 
      template: [
      '<div>',
      '  <div class="draft-buttons" ng-switch on="draftStatus">',
      '    <div ng-switch-when="draftExists">',
      '      <i class="icon icon-lock"></i>',
      '      <span>Draft owner: {{draft.user}}</span>',
      '    </div>',
      '    <button ng-switch-when="canMakeDraft" ng-click="makeADraft(item)" class="btn btn-sm">Make a Draft</button>',
      '    <button ng-switch-when="ownsDraft" ng-click="callEditDraft()" class="btn btn-sm">Edit Draft</button>',
      '    <button ng-click="goLive(item)" ng-show="canGoLive" class="btn btn-sm">Go live</button>',
      '  </div>',
      '  <button ng-click="cloneItem(item)" ng-show"canClone" class="btn btn-sm">Clone</button>',
      '</div>'].join('')
    };
  }]);
