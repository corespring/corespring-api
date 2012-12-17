function MainController($scope, $rootScope, $location, Config, LaunchConfigService, LtiItemService) {

  console.log("MainController");

  var init = function () {
    $scope.configurationId = Config.configurationId;

    console.log("configurationId: " + $scope.configurationId);

    if (!$scope.configurationId) {
      throw "No configurationId defined - can't load configuration";
    }

    LaunchConfigService.get({id: $scope.configurationId}, function (data) {
      $rootScope.config = data;

      if ($scope.config.itemId) {
        $location.url("/view/" + $scope.config.itemId);
      }
      else {
        $location.url("/browse");
      }

    });
  };

  $scope.saveItem = function (onSaveCompleteCallback) {
    LaunchConfigService.save({id: $scope.config.id}, $scope.config, function (data) {
      $scope.config = data;
      if (onSaveCompleteCallback) onSaveCompleteCallback();
    });
  };

  //TODO: Shouldn't need to bind to rootScope
  $rootScope.$on('saveConfig', function (event, object) {

    var doRedirect = (object && object.redirect === false) ? false : true;

    var onSaveCompleted = function () {
      if (!Config.returnUrl.match(/\?/)) {
        Config.returnUrl = Config.returnUrl + "?";
      }
      var args = [];
      args.push("embed_type=basic_lti");
      var url = document.location.href;
      args.push("url=" + encodeURIComponent(url + "?canvas_config_id=" + $scope.config.id));
      location.href = Config.returnUrl + args.join('&');
    };

    if (doRedirect) {
      $scope.saveItem(onSaveCompleted);
    } else {
      $scope.saveItem();
    }

  });

  init();
}

MainController.$inject = ['$scope', '$rootScope', '$location', 'Config', 'LaunchConfigService', 'LtiItemService'];