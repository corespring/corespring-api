(function(root) {

  root.tagger = root.tagger || {};
  root.tagger.HomeController = HomeController;

  HomeController.$inject = [
    '$http',
    '$location',
    '$rootScope',
    '$scope',
    '$timeout',
    'ItemDraftService',
    'ItemFormattingUtils',
    'ItemService',
    'Logger',
    'Modals',
    'UserInfo',
    'V2ItemService',
    'CollectionManager'
  ];

  function HomeController(
    $http,
    $location,
    $rootScope,
    $scope,
    $timeout,
    ItemDraftService,
    ItemFormattingUtils,
    ItemService,
    Logger,
    Modals,
    UserInfo,
    V2ItemService,
    CollectionManager
  ) {

    //Mixin ItemFormattingUtils
    angular.extend($scope, ItemFormattingUtils);

    $http.defaults.headers.get = ($http.defaults.headers.get || {});
    $http.defaults.headers.get['Content-Type'] = 'application/json';

    $rootScope.searchParams = $rootScope.searchParams ? $rootScope.searchParams : ItemService.createWorkflowObject();

    $scope.$root.mode = "home";

    $scope.v2 = new V2();
    $scope.v1 = new V1();

    $scope.cloneItem = cloneItem;
    $scope.deleteItem = deleteItem;
    $scope.edit = edit;
    $scope.hidePopup = hidePopup;
    $scope.launchCatalogView = launchCatalogView;
    $scope.onItemLoad = onItemLoad;
    $scope.publish = publish;
    $scope.isV1 = isV1;

    // Delay in milliseconds for search after item update
    var searchDelay = 1000;

    $scope.delayedSearch = function() {
      $timeout($scope.search, searchDelay);
    };

    $rootScope.$on('editItem', function(ev, itemId) {
      var item = _.find($scope.items, function(i) {
        return i.id === itemId;
      });
      edit(item);
    });

    $rootScope.$broadcast('onListViewOpened');

    init();

    //---------------------------------------------------

    function highlightItem(id) {
      var item = $(document.getElementById('row-'+id));
      var prevPrev = $(document.getElementById('row-'+id)).prev().prev();
      if (prevPrev.length > 0) {
        prevPrev[0].scrollIntoView(true);
      }
      item.addClass('highlighted');
      setTimeout(function() {
        item.removeClass('highlighted');
      }, 200);
    }

    function init() {
      $rootScope.itemData = undefined;
      $scope.userName = UserInfo.userName;
      $scope.org = UserInfo.org;
    }

    function edit(item) {
      route('edit', item);
      $rootScope.$broadcast('editItemOpened', item);
      $rootScope.itemData = item;
    }

    function publish(item, callback) {
      Modals.publish(function(cancelled) {
        if (!cancelled) {
          ItemService.get({
            id: item.id
          }, function(itemData) {
            itemData.publish(
              function success(item) {
                $scope.delayedSearch();
                if(_.isFunction(callback) ) {
                  callback();
                }
              },
              function error(err) {
                alert('Error publishing'); //would be nice if we did something more useful with error messages.
              },
              itemData.id
            );

          });
        }
      });
    }

    function cloneItem(item) {

      function callService(collectionId, open){
        V2ItemService.clone({
          id: item.id,
          collectionId: collectionId
        },
        function success(newItem) {
          if(open){
            var query = isV1(item) ? '?devEditor=true' : '';
            $location.url('/edit/draft/' + newItem.id + query);
          }
          $scope.delayedSearch();
        },
        function error(err) {
          alert('cloning failed: ' + JSON.stringify(err));
        });
      }

      ItemService.get({id: item.id}, function(itemData){
        var collections = CollectionManager.writableCollections();
        Modals.clone(itemData, collections, function(cancelled, open, targetCollection){

          if(!cancelled){
            callService(targetCollection.id, open);
          }
        });
      });
    }

    function isV1(item) {
      return item.apiVersion === 1 || (item.format && item.format.apiVersion === 1);
    }

    function route(action, item) {
      if (isV1(item)) {
        $scope.v1[action](item);
      } else {
        $scope.v2[action](item);
      }
    }

    function V1() {

      this.edit = function(item) {
        if (item.published) {
          Modals.edit(function(cancelled) {
            if (cancelled) {
              return;
            }
            $location.url('/edit/draft/' + item.id + '?devEditor=true');
          });
        } else {
          $location.url('/edit/draft/' + item.id + '?devEditor=true');
        }
      };

      this.publish = function(item) {
        publish(item, function() {
          $scope.v1.itemToPublish = itemData;
          $scope.v1.showConfirmPublishModal = true;
        });
      };

      this.publishConfirmed = function() {
        $scope.v1.showConfirmPublishModal = false;

        $scope.v1.itemToPublish.publish(function(result) {
            if (!result.published) {
              alert('Error publishing');
            }
            $scope.v1.itemToPublish.published = result.published;
            $scope.v1.itemToPublish = null;
          },
          function(err) {
            alert(err);
          });
      };

      this.publishCancelled = function() {
        $scope.v1.itemToPublish = null;
        $scope.v1.showConfirmPublishModal = false;
      };
    }

    function V2() {

      function getItem(id) {
        return _.find($scope.items, function(i) {
          return i.id === id;
        });
      }

      function goToEditDraft(itemId) {
        $location.url('/edit/draft/' + itemId);
      }

      this.edit = function(item) {
        if (item.published) {
          Modals.edit(function(cancelled) {
            if (cancelled) {
              return;
            }
            goToEditDraft(item.id);
          });
        } else {
          goToEditDraft(item.id);
        }
      };

      this.publish = function(item) {
        publish(item);
      };

    }

    function deleteItem(item) {
      Modals['delete'](function(cancelled) {
        if (!cancelled) {
          ItemDraftService.deleteByItemId(
            item.id,
            function draftDeleteByItemIdSuccess(result) {
              ItemService.remove({
                  id: item.id
                },
                function itemRemoveSuccess(result) {
                  Logger.debug('item removed');
                  $scope.delayedSearch();
                },
                function itemRemoveError(err) {
                  Logger.error(err);
                }
              );
            },
            function draftDeleteByItemIdError(err) {
              Logger.error(err);
            });
        }
      });
    }

    function launchCatalogView() {
      openPreview(this.item.id);
    }

    function openPreview(id) {
      $timeout(function() {
        $scope.showPopup = true;
        $scope.popupBg = "extra-large-window";
        $scope.previewingId = id;
        $('#preloader').show();
        $('#player').hide();
      }, 50);

      $timeout(function() {
        $('.window-overlay').scrollTop(0);
      }, 100);
    }

    /**
     * Handlers for the profile player
     */
    function hidePopup() {
      $scope.showPopup = false;
      $scope.previewingId = "";
      $scope.popupBg = "";
    }

    function onItemLoad() {
      $('#preloader').hide();
      $('#player').show();
    }
  }

})(this);