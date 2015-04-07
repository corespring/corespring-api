(function(root) {

  function HomeController($scope,
    $timeout,
    $rootScope,
    $http,
    $location,
    ItemService,
    SearchService,
    CollectionManager,
    Contributor,
    ItemFormattingUtils,
    Logger,
    CmsService,
    UserInfo,
    ItemDraftService) {

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
      loadCollections();
      loadContributors();
      $scope.showDraft = true;
    };

    function loadDraftsForOrg(){
      CmsService.getDraftsForOrg(function(drafts){
        $scope.orgDrafts = drafts;
      }, function error(err){
        console.warn('error: getDraftsForOrg', err);
      });
    }

    $scope.launchCatalogView = function(){
      openPreview(this.item.id);
    };

    function route(action, item){
      if(item.format.apiVersion === 1){
        v1[action](item);
      } else {
        v2[action](item);
      }
    }

    $scope.edit = function(item){
      route('edit', item);
    };
    
    $scope.goLive = function(item){
      route('goLive', item);
    };
    
    $scope.cloneItem = function(item){
      route('cloneItem', item);
    };
    
    $scope.deleteItem = function(item){
      //applies to v1 and v2
    };

    $scope.deleteDraft = function(item){
      //v2 only...
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


    init();
  }

  HomeController.$inject = ['$scope',
    '$timeout',
    '$rootScope',
    '$http',
    '$location',
    'ItemService',
    'SearchService',
    'CollectionManager',
    'Contributor',
    'ItemFormattingUtils',
    'Logger',
    'CmsService',
    'UserInfo',
    'ItemDraftService'
  ];

  root.tagger = root.tagger || {};
  root.tagger.HomeController = HomeController;
})(this);