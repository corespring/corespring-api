angular.module("lti-chooser",
  ['tagger.services',
    'lti-services',
    'ngResource',
    'cs',
    'corespring-directives',
    'corespring-services',
    'corespring-utils',
    'ui']);

angular.module("lti-chooser").config(['$routeProvider', function ($routeProvider) {
    $routeProvider.when('/main', {templateUrl:'/lti/chooser/partials/main', controller:MainController});
    $routeProvider.when('/browse', {templateUrl:'/lti/chooser/partials/browse', controller:BrowseController});
    $routeProvider.when('/view/:itemId', {templateUrl:'/lti/chooser/partials/view', controller: ltiChooser.ViewItemController});
    $routeProvider.otherwise({redirectTo:'/main'});
  }]);

angular.module("lti-services", ['ngResource'])
  .factory("LaunchConfigService", [ '$resource', function($resource){
    return $resource("/lti/launch-config/:id", {}, { save: { method: "PUT"}});
  }])

  .factory("LtiItemService", ['$resource', function($resource){
    return $resource("/api/v1/items/:id", {} );
  }]);


function LtiChooserController( $scope, $rootScope, $location, LaunchConfigService, Config ){

  $scope.returnToSearch = function(){
    $rootScope.item = null;
    $location.url("/browse");
  };

  $scope.showSearch = function(){
     return $location.url() == "/browse";
  };

  $scope.showPager = function(){
    return $location.url().indexOf("/view/") === 0;
  };

  $scope.loadItem = function(id){
    $location.url("/view/" + id);
  };

  $scope.loadMore = function(){
    $rootScope.$broadcast('loadMoreSearchResults');
  };

  $scope.change = function(){
    $location.url("/browse");
  };

  $scope.getAssignRemoveLabel = function(){
    return $scope.isAssigned() ? "Remove" : "Assign";
  };

  $scope.getRemoveTooltip = function(){
    if($scope.isRemoveDisabled()){
      return "Can't unassign because students have interacted with this item";
    }
    return "";
  };

  $scope.isRemoveDisabled = function(){
   return $scope.config && $scope.config.assignments && $scope.config.assignments.length > 0;
  };

  $scope.isAssigned = function(){
    if(!$scope.config){
      return false;
    }

    if(!$scope.config.itemId){
      return false;
    }

    if(!$scope.item){
      return false;
    }

    return $scope.config.itemId === $scope.item.id;
  };

  $scope.remove = function(){

    if($scope.isRemoveDisabled()){
      return;
    }

    $scope.config.itemId = null;
    $rootScope.$broadcast('saveConfig', { redirect: false });
    $location.url("/browse");
  };

  $scope.assign = function(){
    $scope.config.itemId = $scope.item.id;
    $rootScope.$broadcast('saveConfig');
  };

  $scope.init = function(){

    $scope.configurationId = Config.configurationId;

    if (!$scope.configurationId) {
      throw "No configurationId defined - can't load configuration";
    }

    LaunchConfigService.get({id: $scope.configurationId}, function (data) {
      $rootScope.config = data;
    });
  };

  $scope.saveItem = function (onSaveCompleteCallback) {
    LaunchConfigService.save({id: $scope.config.id}, $scope.config, function (data) {
      $scope.config = data;
      if (onSaveCompleteCallback) onSaveCompleteCallback();
    });
  };

  $scope.getCollectionTitle = function(c){
    return c.name;
  };

  $scope.getTitle = function(o){ return o.key };

  $scope.getCollectionSelectedTitle = function(items){
    return getMessage(items);
  };

  $scope.getSelectedTitle = function(items){
    return getMessage(items);
  };

  var getMessage = function(items){
    var msg = "";
    if(items && items.length > 0){
      msg = items.length + " selected";
    } else {
      msg = "0 selected";
    }
    var b = '<button class="btn btn-mini dropdown-toggle nav-button" data-toggle="dropdown">{msg}<span class="caret"></span></button>';
    return b.replace("{msg}", msg);
  };

  $rootScope.$on('saveConfig', function (event, object) {

    var doRedirect = (object && object.redirect === false) ? false : true;

    var onSaveCompleted = function () {
      if (!Config.returnUrl.match(/\?/)) {
        Config.returnUrl = Config.returnUrl + "?";
      }
      var args = [];
      args.push("embed_type=basic_lti");
      var url = document.location.href.replace( document.location.hash, "");
      var encodedUrl =  encodeURIComponent(url + "?canvas_config_id=" + $scope.config.id);
      args.push("url=" + encodedUrl );
      location.href = Config.returnUrl + args.join('&');
    };

    if (doRedirect) {
      $scope.saveItem(onSaveCompleted);
    } else {
      $scope.saveItem();
    }
  });

  $scope.init();
}

LtiChooserController.$inject = ['$scope', '$rootScope', '$location', 'LaunchConfigService', 'Config'];


