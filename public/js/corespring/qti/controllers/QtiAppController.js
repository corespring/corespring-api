function QtiAppController($scope, $timeout, $location, AssessmentSessionService, Config, MessageBridge, Logger) {


  $scope.onMessageReceived = function (e) {

    var obj = JSON.parse(e.data);

    if (obj.message === "submitItem") {
      $scope.$broadcast("submitItem",obj);
    }

    if (obj.message === "update") {
      $scope.originalSettings = angular.copy(obj.settings);

      if ($scope.itemSession) {
        $scope.itemSession.settings = obj.settings;
      } else {
        $scope.pendingSettings = obj.settings;
      }
    }
  };

  MessageBridge.addMessageListener($scope.onMessageReceived);

  $timeout(function () {
    if (typeof(MathJax) != "undefined") {
      MathJax.Hub.Queue(["Typeset", MathJax.Hub]);
    }
  }, 500);

  $scope.reset = function () {
    $scope.$broadcast('reset');
  };

  $scope.init = function () {
    $scope.settingsHaveChanged = false;

    MessageBridge.sendMessage("parent", { message: "ready"});

    var params = {
      itemId: Config.itemId,
      sessionId: Config.sessionId,
      role: Config.role
    };

    if (Config.token) {
      params.access_token = Config.token;
    }

    var onItemSessionLoaded = function (bridgeMessage) {
      return function (data) {
        $scope.itemSession = data;
        $scope.setUpChangeWatcher();
        $scope.settingsHaveChanged = false;
        MessageBridge.sendMessage("parent", {message: bridgeMessage, session: $scope.itemSession});
      }
    };

    if (Config.sessionId === "") {
      AssessmentSessionService.create(params, {}, onItemSessionLoaded("itemSessionCreated"));
    } else {
      AssessmentSessionService.get(params, {}, onItemSessionLoaded("itemSessionRetrieved"));
    }


    $scope.$on('assessmentItem_submit', function (event, itemSession, onSuccess, onError) {

      var params = {
        itemId: itemSession.itemId,
        sessionId: itemSession.id,
        role: Config.role
      };

      AssessmentSessionService.save(params, itemSession, function (data) {
          $scope.itemSession = data;
          onSuccess();
          if (data && data.isFinished) {
            MessageBridge.sendMessage("parent", {message: "sessionCompleted", session: data});
          }
        },
        function (error) {
          //onError logs error
          onError(error);
        });

    });
  };

  /**
   * Track changes to settings so we know if the user needs to save the changes
   * before working with the item.
   */
  $scope.setUpChangeWatcher = function () {

    $scope.originalSettings = angular.copy($scope.itemSession.settings);
    $scope.maxNoOfAttempts = $scope.itemSession.settings.maxNoOfAttempts;

    //need to make sure we store an int from the radio group
    $scope.$watch('itemSession.settings.maxNoOfAttempts', function (newData) {
      $scope.itemSession.settings.maxNoOfAttempts = parseInt(newData);
    });

    //watcher for $watch - builds string from object values
    var watcher = function () {
      var out = "";
      for (var x in $scope.itemSession.settings) {
        out += $scope.itemSession.settings[x];
      }
      return out;
    };

    $scope.$watch(watcher, function (newData) {
      $scope.settingsHaveChanged = !angular.equals(
        $scope.originalSettings,
        $scope.itemSession.settings);

      if ($scope.settingsHaveChanged)
        $scope.$broadcast('controlBarChanged');
    });

  };

  /**
   * Because the current item session has been started - its settings are now locked.
   * So we are going to be creating a new item session.
   */
  $scope.reloadItem = function () {

    MessageBridge.sendMessage("parent", { message: "update", settings: $scope.itemSession.settings });

    AssessmentSessionService.create({itemId: $scope.itemSession.itemId}, $scope.itemSession, function (data) {
      $scope.reset();
      $scope.$broadcast('unsetSelection');
      $scope.itemSession = data;

      MessageBridge.sendMessage("parent", {message: "itemSessionCreated", session: $scope.itemSession});

      if ($scope.pendingSettings) {
        $scope.itemSession.settings = $scope.pendingSettings;
        $scope.pendingSettings = null;
      }

      $scope.setUpChangeWatcher();
      // Empty out the responses
      for (var i = 0; i < $scope.responses.length; i++)
        $scope.responses[i].value = [];
    });
  };

  $scope.init();

}

QtiAppController.$inject = ['$scope', '$timeout', '$location', 'AssessmentSessionService', 'Config', 'MessageBridge', 'Logger'];

