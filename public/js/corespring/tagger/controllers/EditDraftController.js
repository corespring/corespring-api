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
    $location,
    $routeParams,
    $scope,
    $timeout,
    $window,
    ItemDraftService,
    ItemService,
    Logger,
    Modals
  ) {

    var itemService = new ItemService({
      id: $routeParams.itemId
    });

    $scope.containerClassName = '.item-iframe-container';
    $scope.devEditorVisible = false;
    $scope.hasChanges = false;
    $scope.itemId = $routeParams.itemId;
    $scope.unloadMessages = {
      commitInProgress: 'saving in progress - please try again',
      hasChanges: 'There are updates to this item that have not been saved. Are you sure you want to leave?'
    };

    $scope.backToCollections = backToCollections;
    $scope.clone = clone;
    $scope.confirmSaveBeforeLeaving = confirmSaveBeforeLeaving;
    $scope.discardAndLoadFreshCopy = discardAndLoadFreshCopy;
    $scope.discardDraft = discardDraft;
    $scope.ignoreConflict = ignoreConflict;
    $scope.initiallyDiscardAnyDraftAndLoadAFreshCopyOfTheItem = initiallyDiscardAnyDraftAndLoadAFreshCopyOfTheItem;
    $scope.loadDraftItem = loadDraftItem;
    $scope.onItemChanged = onItemChanged;
    $scope.publish = publish;
    $scope.saveBackToItem = saveBackToItem;
    $scope.showDevEditor = showDevEditor;
    $scope.showEditor = showEditor;

    $scope.$watch('commitInProgress', onChangeCommitInProgress);

    $scope.$on('$routeChangeStart', onRouteChangeStart);
    $scope.navigationHooks.beforeUnload = angularBeforeUnload;
    $($window).bind('beforeunload', jqueryBeforeUnload);

    $scope.devEditorVisible = !_.isUndefined($routeParams.devEditor) && $routeParams.devEditor !== 'false';
    //AC-252
    $scope.initiallyDiscardAnyDraftAndLoadAFreshCopyOfTheItem();

    //---------------------------------------------

    function initiallyDiscardAnyDraftAndLoadAFreshCopyOfTheItem() {
      var avoid400ErrorWhenDraftDoesNotExist = true;
      $scope.discardDraft(function () {
        $scope.loadDraftItem();
      }, avoid400ErrorWhenDraftDoesNotExist);
    }

    function jqueryBeforeUnload() {
      //jquery expects the method to return the question string
      if ($scope.commitInProgress) {
        return $scope.unloadMessages.commitInProgress;
      } else if ($scope.hasChanges) {
        return $scope.unloadMessages.hasChanges;
      }
    }

    function removeJqueryBeforeUnloadHandler() {
      $($window).unbind('beforeunload');
    }

    function angularBeforeUnload(callback) {
      removeJqueryBeforeUnloadHandler();

      if ($scope.commitInProgress) {
        return;
      }

      if (!$scope.hasChanges) {
        callback();
        return;
      }

      Modals.confirmSave(function onCloseModal(cancelled) {
        if (!cancelled) {
          $scope.saveBackToItem(callback);
        } else {
          $scope.discardDraft();
          callback();
        }
      });
    }

    function onRouteChangeStart() {
      removeJqueryBeforeUnloadHandler();
      if ($scope.hasChanges && $scope.confirmSaveBeforeLeaving()) {
        $scope.saveBackToItem();
      }
    }

    function confirmSaveBeforeLeaving() {
      return $window.confirm('There are updates to this item that have not been saved. Would you like to save them before you leave?');
    }

    function discardDraft(done, succeedIfDraftDoesNotExist) {
      done = done || function() {};
      ItemDraftService.deleteDraft($scope.itemId, function(data) {
        Logger.debug('draft ' + $scope.itemId + ' deleted');
        done();
      }, function(err) {
        Logger.warn('draft ' + $scope.itemId + ' not deleted');
        done(err);
      }, false, succeedIfDraftDoesNotExist === true);
    }

    function discardAndLoadFreshCopy() {
      ItemDraftService.deleteDraft($routeParams.itemId, function() {
        $scope.loadDraftItem();
      }, function() {
        console.error('An error occurred deleting the draft');
      });
    }

    function backToCollections() {
      if ($scope.hasChanges) {
        Modals.confirmSave(function onCloseModal(cancelled) {
          if (cancelled) {
            $scope.discardDraft(goToCollections);
          } else {
            $scope.saveBackToItem(goToCollections);
          }
        });
      } else {
        goToCollections();
      }

      function goToCollections() {
        $scope.hasChanges = false;
        $location.path("/home").search('');
      }
    }

    function saveBackToItem(done) {
      if ($scope.draftIsConflicted) {
        Modals.saveConflictedDraft(function onCloseModal(cancelled) {
          if (!cancelled) {
            commit(true, done);
          }
        });
      } else {
        commit(false, done);
      }
      $scope.hasChanges = false;
    }

    function onChangeCommitInProgress() {
      $scope.navDisabled = $scope.commitInProgress;
    }

    function commit(force, done) {
      done = done || function() {};
      $scope.commitInProgress = true;

      $scope.v2Editor.forceSave(function onSave(err) {
        ItemDraftService.commit($scope.itemId, force, function success() {
          $scope.draftIsConflicted = false;
          $scope.commitInProgress = false;
          $scope.$broadcast('commitComplete');
          done();
        }, function error(err) {
          Logger.warn(err);
          Modals.commitFailedDueToConflict(function onCloseModal(cancelled) {
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

    function clone() {
      $scope.showProgressModal = true;
      ItemDraftService.clone($scope.itemId, function onClone(result) {
        Logger.info(result);
        $scope.showProgressModal = false;
        $location.path('/edit/draft/' + result.itemId);
      }, function onError(err) {
        Logger.error(err);
        $scope.showProgressModal = false;
      });
    }

    function publish() {
      Modals.publish(
        function onCloseModal(cancelled) {
          if (cancelled) {
            return;
          }

          commit(false, function onCommitComplete() {
            itemService.publish(function success() {
                $scope.backToCollections();
              },
              function err(e) {
                Logger.error('Error publishing: ', e);
              },
              $scope.itemId);
          });
        });
    }

    function loadEditor(devEditor) {
      if ($scope.v2Editor) {
        $scope.v2Editor.remove();
      }

      var opts = {
        itemId: $scope.itemId,
        draftName: $scope.draft.user,
        onItemChanged: $scope.onItemChanged,
        devEditor: devEditor,
        autosizeEnabled: true,
        iframeScrollingEnabled: false,
        hideSaveButton: true
      };

      return new org.corespring.players.DraftEditor($scope.containerClassName, opts, function(e) {
        Logger.error(e);
      });
    }

    function loadDraftItem(ignoreConflict) {
      ignoreConflict = ignoreConflict === true;

      $scope.sessionCount = "?";
      itemService.countSessionsForItem(function(o) {
        $scope.sessionCount = o.sessionCount;
      }, function() {
        Logger.warn(err);
      }, $routeParams.id);

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

          //What does this warning mean?
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
    }

    function ignoreConflict() {
      $scope.loadDraftItem(true);
    }

    function showDevEditor() {
      //save changes before switching to editor
      $scope.v2Editor.forceSave(function onSave(err) {
        //make sure the change becomes visible in the ui
        $scope.$apply(function() {
          $scope.devEditorVisible = true;
          $scope.v2Editor = loadEditor(true);
        });
      });
    }

    function showEditor() {
      //save changes before switching to editor
      $scope.v2Editor.forceSave(function onSave(err) {
        //make sure the change becomes visible in the ui
        $scope.$apply(function() {
          $scope.devEditorVisible = false;
          $scope.v2Editor = loadEditor(false);
        });
      });
    }

    function onItemChanged() {
      $scope.$apply(function() {
        $scope.hasChanges = true;
      });
    }

  }

  EditDraftController.$inject = [
    '$location',
    '$routeParams',
    '$scope',
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