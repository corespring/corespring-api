window.ltiChooser = (window.ltiChooser || {});

ltiChooser.ViewItemController = function ($scope, $rootScope, $routeParams, $location, LtiItemService, ServiceLookup, MessageBridge){

  $scope.previewPageIsReady = false;

  $scope.onMessageReceived = function(e){

    var data = JSON.parse(e.data);

    if(data.message === "ready"){
      $scope.previewPageIsReady = true;
      $scope.sendSessionSettings();
    } else if(data.message === "update"){
      $scope.config.sessionSettings = data.settings;
      $rootScope.$broadcast('saveConfig', {redirect: false});
    }
  };

  MessageBridge.addMessageListener($scope.onMessageReceived);

  $scope.sendSessionSettings = function(){
    if(!$scope.previewPageIsReady || !$scope.config)  {
      return;
    }
    MessageBridge.sendMessage("previewIframe", {message:"update", settings: $scope.config.sessionSettings});
  };

  $rootScope.$watch("config", function(newValue){
    if(newValue){
      $scope.sendSessionSettings();
    }
  });

  $scope.previewItemUrl = null;
  $rootScope.item = null;

  $scope.changePanel = function(name){
    $scope.currentSm = null;
    $scope.currentPanel = name;
  };

  $scope.changeSupportingMaterialPanel = function (sm) {
    $scope.changePanel(sm.name);
    $scope.currentSm = sm;
  };


  $scope.getItemUrl = function(){
    if (!$scope.item ) return null;
    return WebRoutes.web.controllers.ShowResource.renderDataResource($scope.item.id).url;
    //return "/web/show-resource/" + $scope.item.id + "/data/main";
  };


  $scope.getSmSrc = function (sm, forPrinting) {
    var templateUrl = ServiceLookup.getUrlFor(forPrinting ? 'printResource' : 'renderResource');
    var key = $scope.item.id + "/" + sm.name;
    //empty it so we trigger a refresh
    return templateUrl.replace("{key}", key);
  };

  LtiItemService.get({id: $routeParams.itemId}, function onSuccess(data){
     $rootScope.item = data;
     $scope.changePanel('profile');
     $scope.previewItemUrl = $scope.getItemUrl();
     console.log("previewItemUrl: " + $scope.previewItemUrl);
  });

};


ltiChooser.ViewItemController.$inject = ['$scope', '$rootScope', '$routeParams', '$location', 'LtiItemService', 'ServiceLookup', 'MessageBridge'];