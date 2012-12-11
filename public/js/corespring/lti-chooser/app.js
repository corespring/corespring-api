angular.module("lti-chooser", ['lti-services', 'ngResource']);

angular.module("lti-services", ['ngResource'])
  .factory("LaunchConfigService", [ '$resource', function($resource){
    return $resource("/lti/launch-config/:id", {}, { save: { method: "PUT"}});
  }])

  .factory("ItemService", ['$resource', function($resource){
    return $resource("/api/v1/items/:id", {} );
  }]);



function LtiChooserController($scope, Config, LaunchConfigService, ItemService){
  //console.log("lti chooser controller: " + $scope + $resource, Config);

  $scope.updateHasItem = function(){

    $scope.hasItem  = $scope.config.itemId !== undefined;

    if($scope.hasItem){
      ItemService.get({id:$scope.config.itemId}, function(data){
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

  $scope.configurationId = Config.configurationId;

  $scope.settings = Config.settings;

  $scope.saveItem = function(){
    LaunchConfigService.save( {id: $scope.config.id}, $scope.config, function(data){
      $scope.config = data;
      $scope.updateHasItem();
    });
  };

  $scope.change = function(){
    $scope.config.itemId = null;
    $scope.hasItem = false;
  };

  $scope.done = function(){

    if(!Config.returnUrl.match(/\?/)) {
      Config.returnUrl = Config.returnUrl + "?";
    }

    var args = [];
    args.push("embed_type=basic_lti");
    var url = document.location.href;
    args.push("url=" + encodeURIComponent(url));
    location.href = Config.returnUrl + args.join('&');
  };

  init();
}

LtiChooserController.$inject = ['$scope', 'Config', 'LaunchConfigService', 'ItemService'];