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

      if($scope.commitInProgress) {
        return;
      } else if ($scope.hasChanges) {
        Modals.confirmSave(function(cancelled) {
          if (!cancelled) {
            $scope.saveBackToItem(callback);
          } else {
            $scope.discardDraft();
            callback();
          }
        });
      } else {
        callback();
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

    $scope.$watch('commitInProgress', function(){
      $scope.navDisabled = $scope.commitInProgress;
    });

    function commit(force, done) {

      done = done || function() {};

      $scope.commitInProgress = true;

      $scope.v2Editor.forceSave(function(err){
        ItemDraftService.commit($scope.itemId, force, function success() {
          $scope.draftIsConflicted = false;
          $scope.commitInProgress = false;
          $scope.$broadcast('commitComplete');
          done();
        }, function error(err) {
          Logger.warn(err);
          Modals.commitFailedDueToConflict(function(cancelled) {
            $scope.draftIsConflicted = true;
            $scope.commitInProgress = false;
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


    $scope.containerClassName = '.item-iframe-container';

    function loadEditor(devEditor){
     
      if($scope.v2Editor){
        $scope.v2Editor.remove();
      } 

      var opts = {
        itemId: $scope.itemId,
        draftName: $scope.draft.user,
        onItemChanged: $scope.onItemChanged,
        onProfileSaved: $scope.onProfileSaved,
        devEditor: devEditor,
        autosizeEnabled: false
      };

      return new org.corespring.players.DraftEditor($scope.containerClassName, opts, function(e){
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

    $scope.onProfileSaved = function() {
      $scope.$apply(function() {
        $scope.hasChanges = false;
      });
    };

    $scope.unloadMessages = {
      commitInProgress: 'saving in progress - please try again',
      hasChanges: 'There are updates to this item that have not been saved. Are you sure you want to leave?'
    };

    function onBeforeUnload(){
      if($scope.commitInProgress){
        return $scope.unloadMessages.commitInProgress;
      } else if($scope.hasChanges){
        return $scope.unloadMessages.hasChanges;
      }
    }
    
    var bindBeforeUnloadHandler = _.once(function(){
      $($window).bind('beforeunload', onBeforeUnload);
    });

    bindBeforeUnloadHandler();

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
