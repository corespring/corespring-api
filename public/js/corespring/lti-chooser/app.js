angular.module("lti-chooser", ['lti-services', 'ngResource']);

angular.module("lti-services", ['ngResource'])
  .factory("LaunchConfigService", [ '$resource', function($resource){
    return $resource("/lti/launch-config/:id", {}, { save: { method: "PUT"}});
  }]);



function LtiChooserController($scope, Config, LaunchConfigService){
  //console.log("lti chooser controller: " + $scope + $resource, Config);

  var init = function(){
    LaunchConfigService.get({id: $scope.configurationId}, function(data){
      $scope.config = data;
      $scope.hasItem = $scope.config.itemId != undefined;
    });
  };

  $scope.configurationId = Config.configurationId;

  $scope.settings = Config.settings;

  $scope.saveItem = function(){
    LaunchConfigService.save( {id: $scope.config.id}, $scope.config, function(data){
      $scope.config = data;
    });
  };

  init();
}

LtiChooserController.$inject = ['$scope', 'Config', 'LaunchConfigService'];