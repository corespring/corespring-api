function IframedAppController($scope, AssessmentSessionService, Config, MessageBridge) {

  var params = {
    itemId: Config.itemId,
    sessionId: Config.sessionId};

  if (Config.token) {
    params.access_token = Config.token;
  }

  this.onItemSessionLoaded = function (data) {
    $scope.itemSession = data;

    var message = {
      message: "itemSessionLoaded",
      session: $scope.itemSession
    };

    MessageBridge.sendMessage("parent", message);
  };

  this.updateItemSessionSettings = function (obj) {
    $scope.settings = obj.settings;
    AssessmentSessionService.create(params, $scope.settings, this.onItemSessionLoaded);
  };

  $scope.onMessageReceived = angular.bind(this, function (e) {
    var obj = JSON.parse(e.data);
    switch (obj.message) {
      case "updateItemSessionSettings" :
        this.updateItemSessionSettings(obj);
        break;

      default:
        console.log("IframedAppController - unknown message: " + obj.message);
        break;

    }
  });

  MessageBridge.addMessageListener($scope.onMessageReceived);

  MessageBridge.sendMessage("parent", { message: "ready"});


  $scope.$on('assessmentItem_submit', function (event, itemSession, onSuccess, onError) {

    var params = {
      itemId: itemSession.itemId,
      sessionId: itemSession.id,
      access_token: Config.token
    };

    AssessmentSessionService.save(params, itemSession, function (data) {
        $scope.itemSession = data;
        onSuccess();
        MessageBridge.sendMessage("parent", {message: "sessionCompleted", session: data});
      },
      function (error) {
        onError(error)
      });

  });
}

IframedAppController.$inject = [
  '$scope',
  'AssessmentSessionService',
  'Config',
  'MessageBridge'];

