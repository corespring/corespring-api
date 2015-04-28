(function(root) {

  function HomeController($scope,
    $timeout,
    $rootScope,
    $http,
    $location,
    ItemService,
    ItemFormattingUtils,
    Logger,
    UserInfo,
    ItemDraftService,
    V2ItemService,
    Modals) {

    //Mixin ItemFormattingUtils
    angular.extend($scope, ItemFormattingUtils);

    $http.defaults.headers.get = ($http.defaults.headers.get || {});
    $http.defaults.headers.get['Content-Type'] = 'application/json';

    $scope.$root.mode = "home";

    $rootScope.searchParams = $rootScope.searchParams ? $rootScope.searchParams : ItemService.createWorkflowObject();
    $rootScope.$broadcast('onListViewOpened');

    var init = function() {
      $scope.userName = UserInfo.userName;
      $scope.org = UserInfo.org;
      loadDraftsForOrg();
    };

    function loadDraftsForOrg(){
      ItemDraftService.getDraftsForOrg(function(drafts){
        $scope.orgDrafts = drafts;
      }, function error(err){
        console.warn('error: getDraftsForOrg', err);
      });
    }

    function V1(){

      this.edit = function(item){
        $location.url('/old/edit/' + item.id );
      };

      this.cloneItem = function(item){
        //The item passed in is not coming from the v1 ItemService
        //and therefore doesn't have the clone method. ItemService.get
        //does that for us.
        ItemService.get({id: item.id}, function(itemData){
          itemData.clone(
            function success(newItem){
              $location.url('/old/edit/' + newItem.id );
            },
            function error(err){
              alert('cloneItem:', JSON.stringify(err));
            }
          );
        });
      };

      this.publish = function(item){
        //The item passed in is not coming from the v1 ItemService
        //and therefore doesn't have the clone method. ItemService.get
        //does that for us.
        ItemService.get({id: item.id}, function(itemData){
          $scope.v1.itemToPublish = itemData;
          $scope.v1.showConfirmPublishModal = true;
        });
      };

      this.publishConfirmed = function(){
        $scope.v1.showConfirmPublishModal = false;

        $scope.v1.itemToPublish.publish(function(result){
          if(!result.published){
            alert('Error publishing');
          }
          $scope.v1.itemToPublish.published = result.published;
          $scope.v1.itemToPublish = null;
        },
        function(err){
          alert(err);
        });
      };

      this.publishCancelled  = function(){
        $scope.v1.itemToPublish = null;
        $scope.v1.showConfirmPublishModal = false;
      };
    }

    function V2(){

      function getItem(id){
        return _.find($scope.items, function(i){
          return i.id === id;
        });
      }

      function goToEditDraft(itemId){
        $location.url('/edit/draft/' + itemId);
      }

      this.edit = function(item){
        if(item.published){
          Modals.edit(function(cancelled){
            if(cancelled){
              return;
            }
            goToEditDraft(item.id);
          });
        } else {
          goToEditDraft(item.id);
        }
      };

      this.publish = function(item){
        item.publish(
          function(){
            $scope.search();
          },
          function(err){
            Logger.error(err);
          }
        );
      };

      this.cloneItem = function(item){
        V2ItemService.clone({id: item.id},
          function success(newItem){
            goToEditDraft(newItem.id);
          },
          function error(err){
            alert('cloneItem:', JSON.stringify(err));
          }
        );
      };
    }

    $scope.v2 = new V2();
    $scope.v1 = new V1();

    $scope.launchCatalogView = function(){
      openPreview(this.item.id);
    };

    function route(action, item){
      if (item.apiVersion === 1) {
        $scope.v1[action](item);
      } else {
        $scope.v2[action](item);
      }
    }

    $scope.edit = function(item){
      route('edit', item);
    };

    $scope.publish = function(item){
      Modals.publish(function(cancelled){
        if(!cancelled){
          route('publish', item);
        }
      });
    };

    $scope.cloneItem = function(item){
      route('cloneItem', item);
    };

    $scope.deleteItem = function(item) {
      Modals['delete'](function(cancelled){
        if(!cancelled){
          ItemDraftService.deleteByItemId(
            item.id, 
            function draftsDeleted(result){
              ItemService.remove(
                { id: item.id },
                function(result) {
                  Logger.debug('item removed');
                  $timeout($scope.search, 1000);
                },
                function error(err){
                  Logger.error(err);
                }
              );
            }, 
            function error(err){
              Logger.error(err);
            });
        }
      });
    };

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

    $scope.hidePopup = function() {
      $scope.showPopup = false;
      $scope.previewingId = "";
      $scope.popupBg = "";
    };

    $scope.onItemLoad = function() {
      $('#preloader').hide();
      $('#player').show();
    };


    init();
  }

  HomeController.$inject = ['$scope',
    '$timeout',
    '$rootScope',
    '$http',
    '$location',
    'ItemService',
    'ItemFormattingUtils',
    'Logger',
    'UserInfo',
    'ItemDraftService',
    'V2ItemService',
    'Modals'
  ];

  root.tagger = root.tagger || {};
  root.tagger.HomeController = HomeController;
})(this);
