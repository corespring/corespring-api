window.ltiChooser = (window.ltiChooser || {});

ltiChooser.ViewItemController = function ($scope, $rootScope, $routeParams, $location, LtiItemService, ServiceLookup, MessageBridge, ItemFormattingUtils){

  $scope.prependHttp = ItemFormattingUtils.prependHttp;
  
  $scope.previewPageIsReady = false;

  $scope.onMessageReceived = function(e){

    var data = JSON.parse(e.data);

    if(data.message === "ready"){
      $scope.previewPageIsReady = true;
      $scope.sendSessionSettings();
    } else if(data.message === "update"){
      $scope.quiz.question.settings = data.settings;
      $rootScope.$broadcast('saveConfig', {redirect: false});
    }
  };

  MessageBridge.addMessageListener($scope.onMessageReceived);

  $scope.sendSessionSettings = function(){
    if(!$scope.previewPageIsReady || !$scope.quiz)  {
      return;
    }
    MessageBridge.sendMessage("previewIframe", {message:"update", settings: $scope.quiz.question.settings});
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

    var baseUrl = ServiceLookup.getUrlFor('playerPreview');
    return baseUrl.replace(":itemId", $scope.item.id);
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
  });

};


ltiChooser.ViewItemController.$inject = ['$scope', '$rootScope', '$routeParams', '$location', 'LtiItemService', 'ServiceLookup', 'MessageBridge', 'ItemFormattingUtils'];