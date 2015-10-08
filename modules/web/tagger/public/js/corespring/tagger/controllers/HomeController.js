(function (root) {

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
    'V2ItemService'
  ];

  function HomeController($http,
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
                          V2ItemService) {

    //Mixin ItemFormattingUtils
    angular.extend($scope, ItemFormattingUtils);

    $http.defaults.headers.get = ($http.defaults.headers.get || {});
    $http.defaults.headers.get['Content-Type'] = 'application/json';

    $rootScope.searchParams = $rootScope.searchParams ? $rootScope.searchParams : ItemService.createWorkflowObject();

    $scope.$root.mode = "home";


    $scope.cloneItem = cloneItem;
    $scope.deleteItem = deleteItem;
    $scope.edit = editItem;
    $scope.hidePopup = hidePopup;
    $scope.launchCatalogView = launchCatalogView;
    $scope.onItemLoad = onItemLoad;
    $scope.publish = publishItem;

    // Delay in milliseconds for search after item update
    var searchDelay = 1000;

    $scope.delayedSearch = function () {
      $timeout($scope.search, searchDelay);
    };

    $rootScope.$broadcast('onListViewOpened');

    init();

    //---------------------------------------------------

    function init() {
      $scope.userName = UserInfo.userName;
      $scope.org = UserInfo.org;
    }

    function editItem(item) {
      if (isV1Item(item)) {
        $location.url('/edit/' + item.id);
      } else {
        if (item.published) {
          Modals.edit(function (cancelled) {
            if (cancelled) {
              return;
            }
            goToEditDraft(item.id);
          });
        } else {
          goToEditDraft(item.id);
        }
      }
    }

    function cloneItem(item) {
      V2ItemService.clone({
          id: item.id
        },
        function success(newItem) {
          goToEditDraft(newItem.id);
        },
        function error(err) {
          alert('cloneItem:', JSON.stringify(err));
        }
      );
    }

    function publishItem(item, callback) {
      Modals.publish(function (cancelled) {
        if (!cancelled) {
          V2ItemService.publish({id: item.id}, function success() {
            $scope.delayedSearch();
            if (_.isFunction(callback)) {
              callback();
            }
          }, function error(e) {
            alert(e);
          });
        }
      });
    }

    function isV1Item(item) {
      return item.apiVersion === 1 || (item.format && item.format.apiVersion === 1);
    }

    function goToEditDraft(itemId) {
      $location.url('/edit/draft/' + itemId);
    }

    function deleteItem(item) {
      Modals['delete'](function (cancelled) {
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
      $timeout(function () {
        $scope.showPopup = true;
        $scope.popupBg = "extra-large-window";
        $scope.previewingId = id;
        $('#preloader').show();
        $('#player').hide();
      }, 50);

      $timeout(function () {
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