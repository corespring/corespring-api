angular.module("lti-chooser", ['tagger.services','lti-services', 'ngResource']);

angular.module("lti-services", ['ngResource'])
  .factory("LaunchConfigService", [ '$resource', function($resource){
    return $resource("/lti/launch-config/:id", {}, { save: { method: "PUT"}});
  }])

  .factory("LtiItemService", ['$resource', function($resource){
    return $resource("/api/v1/items/:id", {} );
  }]);


function SearchController($scope, $rootScope, $http, ItemService, SearchService, Collection) {
  $http.defaults.headers.get = ($http.defaults.headers.get || {});
  $http.defaults.headers.get['Content-Type'] = 'application/json';

  $scope.searchParams = $rootScope.searchParams ? $rootScope.searchParams : ItemService.createWorkflowObject();

  var init = function(){
    //$scope.search();
    loadCollections();
  };

  $scope.search = function() {
    $rootScope.$broadcast("beginSearch");
    SearchService.search($scope.searchParams, function(res){
      $rootScope.items = res;
    });
  };

  $scope.loadMore = function () {
    SearchService.loadMore(function () {
        // re-bind the scope collection to the services model after result comes back
        $rootScope.items = SearchService.itemDataCollection;
      }
    );
  };

  function loadCollections() {
    Collection.get({}, function (data) {
        $scope.collections = data;
      },
      function () {
        console.log("load collections: error: " + arguments);
      });
  }
  init();
}
SearchController.$inject = ['$scope',
  '$rootScope',
  '$http',
  'ItemService',
  'SearchService',
  'Collection'];


function LtiChooserController($scope, Config, LaunchConfigService, LtiItemService){
  //console.log("lti chooser controller: " + $scope + $resource, Config);

  $scope.showTip = true;
  $scope.mode = "start";

  $scope.$on('beginSearch', function(event, items){
    $scope.mode = "find";
  });

  $scope.$on('searchCompleted', function(event, items){
    $scope.items = items;
  });

  $scope.updateHasItem = function(){

    $scope.mode = ($scope.config.itemId !== undefined) ? 'hasItem' : 'start';

    if($scope.mode == 'hasItem'){
      LtiItemService.get({id:$scope.config.itemId}, function(data){
        $scope.item = data;
      });
    }
  };

  var init = function(){
    LaunchConfigService.get({id: $scope.configurationId}, function(data){
      $scope.config = data;
      $scope.updateHasItem();
    });
  };

  $scope.selectItem = function(item){
    $scope.config.itemId = item.id;
    $scope.updateHasItem();
  };

  $scope.configurationId = Config.configurationId;

  $scope.settings = Config.settings;

  $scope.saveItem = function( onSaveCompleteCallback ){
    LaunchConfigService.save( {id: $scope.config.id}, $scope.config, function(data){
      $scope.config = data;
      $scope.updateHasItem();

      if(onSaveCompleteCallback) onSaveCompleteCallback();
    });
  };

  $scope.change = function(){
    $scope.config.itemId = null;
    $scope.mode = 'start';
  };

  $scope.done = function(){

    $scope.saveItem( function(){
      if(!Config.returnUrl.match(/\?/)) {
        Config.returnUrl = Config.returnUrl + "?";
      }

      var args = [];
      args.push("embed_type=basic_lti");
      var url = document.location.href;
      args.push("url=" + encodeURIComponent(url + "?canvas_config_id=" + $scope.config.id));
      location.href = Config.returnUrl + args.join('&');
    });
  };

  $scope.getNumberOfAssignments = function(config){
    if(config && config.assignments) return config.assignments.length;
    return 0;
  };

  init();
}

LtiChooserController.$inject = ['$scope', 'Config', 'LaunchConfigService', 'LtiItemService'];