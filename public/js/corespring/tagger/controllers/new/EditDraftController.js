(function(root) {
  'use strict';

  /**
   * Remove Item utility method
   * TODO Quite dangerous to put this into the array prototype
   */
  if (Array.prototype.removeItem === null) Array.prototype.removeItem = function(item) {
    var itemIndex = this.indexOf(item);

    if (itemIndex == -1) {
      return null;
    }

    return this.splice(itemIndex, 1)[0];
  };

  /**
   * Controller for editing Item
   */
  function EditDraftController(
    $scope,
    $location,
    $routeParams,
    $timeout,
    $window,
    ItemDraftService,
    ItemService,
    Logger,
    Modals) {


    $scope.devEditorVisible = false;

    var itemService = new ItemService({
      id: $routeParams.itemId
    });

    $scope.navigationHooks.beforeUnload = function(callback) {
      $($window).unbind('beforeunload');
      if (!$scope.hasChanges) {
        callback();
      } else {
        Modals.confirmSave(function(cancelled) {
          if (!cancelled) {
            $scope.saveBackToItem(callback);
          } else {
            $scope.discardDraft();
            callback();
          }
        });
      }
    };

    $scope.discardDraft = function(){
      ItemDraftService.deleteDraft($scope.itemId, function(data){
        Logger.log('draft ' + $scope.itemId + ' deleted');
      }, function(err){
        Logger.warn('draft ' + $scope.itemId + ' not deleted');
      });
    };

    $scope.confirmSaveBeforeLeaving = function() {
      return $window.confirm('There are updates to this item that have not been saved. Would you like to save them before you leave?');
    };

    $scope.$on('$routeChangeStart', function() {
      $($window).unbind('beforeunload');
      if ($scope.hasChanges && $scope.confirmSaveBeforeLeaving()) {
        $scope.saveBackToItem();
      }
    });

    $scope.backToCollections = function() {
      if ($scope.hasChanges) {
        Modals.confirmSave(function(cancelled) {
          if (!cancelled) {
            $scope.saveBackToItem();
          } else {
            $scope.discardDraft();
          }
          $scope.hasChanges = false;
          $location.path("/home").search('');
        });
      } else {
        $location.path("/home").search('');
      }
    };

    $scope.itemId = $routeParams.itemId;
    $scope.hasChanges = false;

    $scope.saveBackToItem = function(done) {
      if ($scope.draftIsConflicted) {
        Modals.saveConflictedDraft(function(cancelled) {
          if (!cancelled) {
            commit(true, done);
          }
        });
      } else {
        commit(false, done);
      }
      $scope.hasChanges = false;
    };

    function commit(force, done) {

      done = done || function() {};

      $scope.isSaveDone = false;
      $scope.showProgressModal = true;
      $scope.v2Editor.forceSave(function(err){
        ItemDraftService.commit($scope.itemId, force, function success() {
          Logger.info('commit successful');
          $scope.draftIsConflicted = false;
          $scope.isSaveDone = true;
          $scope.showProgressModal = false;
          $timeout(function() {
            $scope.isSaveDone = false;
          }, 3000);
          done();
        }, function error(err) {
          Logger.warn(err);
          Modals.commitFailedDueToConflict(function(cancelled) {
            $scope.draftIsConflicted = true;
            $scope.showProgressModal = false;
            if (cancelled) {
              done();
              return;
            }
            commit(true, done);
          });
        });
      });
    }

    $scope.clone = function() {
      $scope.showProgressModal = true;
      ItemDraftService.clone($scope.itemId, function(result) {
        Logger.info(result);
        $scope.showProgressModal = false;
        $location.path('/edit/draft/' + result.itemId);
      }, function(err) {
        Logger.error(err);
        $scope.showProgressModal = false;
      });
    };

    $scope.publish = function() {
      Modals.publish(
        function(cancelled) {
          if (cancelled) {
            return;
          }

          commit(false, function() {
            itemService.publish(function success() {
                $scope.backToCollections();
              },
              function err(e) {
                Logger.error('Error publishing: ', e);
              },
              $scope.itemId);
          });
        });
    };


    function loadEditor(devEditor){
     
      if($scope.v2Editor){
        $scope.v2Editor.remove();
      } 

      var opts = {
        itemId: $scope.itemId,
        draftName: $scope.draft.user,
        onItemChanged: $scope.onItemChanged,
        devEditor: devEditor
      };

      return new org.corespring.players.DraftEditor('.draft-editor-holder', opts, function(e){
        Logger.error(e);
      });
    }

    $scope.loadDraftItem = function(ignoreConflict) {

      ignoreConflict = ignoreConflict === true;

      ItemDraftService.get({
          id: $routeParams.itemId,
          ignoreConflict: ignoreConflict
        },
        function onItemLoaded(draft) {
          $scope.showConflictError = false;
          $scope.draft = draft;
          $scope.itemId = draft.itemId;
          $scope.baseId = $scope.itemId.indexOf(':') !== -1 ? $scope.itemId.split(':')[0] : $scope.itemId;
          $scope.version = $scope.itemId.indexOf(':') !== -1 ? $scope.itemId.split(':')[1] : '';
          console.warn('ItemSessionCount doesn\'t apply for a user draft');
          $scope.v2Editor = loadEditor($scope.devEditorVisible); 
          $scope.draftIsConflicted = ignoreConflict;
        },
        function onError(err, statusCode) {
          if (statusCode === 409) {
            $scope.showConflictError = true;
          } else {
            console.error('An error has occured', err);
          }
        });
    };

    $scope.discard = function() {
      ItemDraftService.deleteDraft($routeParams.itemId, function() {
        $scope.loadDraftItem();
      }, function() {
        console.error('An error occured deleting the draft');
      });
    };

    $scope.ignoreConflict = function() {
      $scope.loadDraftItem(true);
    };

    $scope.showDevEditor = function() {
      $scope.devEditorVisible = true;
      $scope.v2Editor = loadEditor(true); 
    };

    $scope.showEditor = function() {
      $scope.devEditorVisible = false;
      $scope.v2Editor = loadEditor(false);
    };

    $scope.onItemChanged = function() {
      $scope.$apply(function() {
        $scope.hasChanges = true;
      });
    };

    if (!$scope.hasBoundBeforeUnload) {
      $($window).bind('beforeunload', function() {
        return $scope.hasChanges ? "There are updates to this item that have not been saved. Are you sure you want to leave?" : undefined;
      });
      $scope.hasBoundBeforeUnload = true;
    }

    $scope.loadDraftItem();
  }
  EditDraftController.$inject = [
  '$scope',
  '$location',
  '$routeParams',
  '$timeout',
  '$window',
  'ItemDraftService',
  'ItemService',
  'Logger',
  'Modals'
];

  root.tagger = root.tagger || {};
  root.tagger.EditDraftController = EditDraftController;

})(this);
