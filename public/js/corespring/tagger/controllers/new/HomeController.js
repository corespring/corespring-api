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
        item.clone(
          function success(newItem){
            $location.url('/old/edit/' + newItem.id );
          }, 
          function error(err){
            alert('cloneItem:', JSON.stringify(err));
          }
        );
      };

      this.publish = function(item){
        $scope.v1.itemToPublish = item;
        $scope.v1.showConfirmPublishModal = true;
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

      function goToEditDraft(draftId){
        $location.url('/edit/draft/' + draftId);
      }

      function makeADraft(itemId, onSuccess){
        ItemDraftService.createUserDraft(itemId, 
          function(draft){
            $scope.orgDrafts.push(draft);
            onSuccess(draft.id);
          }, 
          function error(err){
            alert('error making a draft' + JSON.stringify(err));
          }
        );
      }

      this.edit = function(item){
        Modals.edit(function(cancelled){
          
          if(cancelled){
            return;
          }

          var draft = _.find($scope.orgDrafts, function(d){
            return d.itemId === item.id;
          });

          if(draft){
            goToEditDraft(draft.id);
          } else {
            makeADraft(item.id, goToEditDraft); 
          }
        });
      };

      this.publish = function(item){

        var draft = _.find($scope.orgDrafts, function(d){
          return d.itemId == item.id;
        });

        if(!draft){
          Logger.warn('can\'t find draft for item: item.id, going to just publish the item');
          /**
           * Not sure if this is the correct behaviour - will check w/ gwen/whitney
           */
          item.publish(
            function(){
              $scope.search();
            }, 
            function(err){
              Logger.error(err);
            }
          );
        } else {
          ItemDraftService.publish(draft.id, 
            function(result){
              $scope.search();
            }, 
            function(err){
              Logger.error(err);
            }
          );
        }
      };

      this.cloneItem = function(item){
        V2ItemService.clone({id: item.id}, 
          function success(newItem){
            makeADraft(newItem.id, goToEditDraft);
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
      if(item.format.apiVersion === 1){
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
          ItemService.remove({ id: item.id },
            function(result) {
              $scope.search();
            });
        }
      });
    };

    $scope.deleteDraft = function(draft){
      ItemDraftService.deleteDraft(draft.id, 
        function(result){
          console.log('deleting draft, successful');
          $scope.orgDrafts = _.reject($scope.orgDrafts, function(d){
            return d.id === draft.id;
          });
        }, 
        function(err){
          console.warn('Error deleting draft');
        }
      );
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