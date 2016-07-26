function SwitcherCtrl($scope, DisplayProvider, ColorTools) {
  var defaults;

  // Monkeypatch underscore
  if (!_.cloneDeep) {
    _.cloneDeep = function(obj) {
      return JSON.parse(JSON.stringify(obj));
    };
  }

  $scope.config = {};

  DisplayProvider.get(function(config) {
    $scope.config = config;
    defaults = _.cloneDeep($scope.config);
    console.log($scope.config);
  });

  function dashize(string) {
    return string.replace(/([a-z])([A-Z])/g, '$1-$2').toLowerCase()
  }

  $scope.$on('settingsReset', function() {
    $scope.config = _.cloneDeep(defaults);
  });

  $scope.$on('settingsSave', function() {
    DisplayProvider.set($scope.config, function() {
      window.alert("Your settings have been saved.");
    });
  });

  $scope.$on('settingsDefault', function() {
    DisplayProvider.defaults(function(defaults) {
      $scope.config = defaults;
    });
  });

  $scope.lighten = function() {
    function lighten(hex) {
      return '#' + ColorTools.lighten(hex, 0.45).toLowerCase();
    }
    $scope.config.colors.correctLight = lighten($scope.config.colors.correctDark);
    $scope.config.colors.incorrectLight = lighten($scope.config.colors.incorrectDark);
    $scope.config.colors.nothingSubmittedLight = lighten($scope.config.colors.nothingSubmittedDark);
    $scope.config.colors.partiallyCorrectLight = lighten($scope.config.colors.partiallyCorrectDark);
  };

  $scope.updateStylesheet = function() {
    var stylesheetId = 'color-styles';

    function stylesheetContent() {
      return _.map($scope.config.colors, function(color, key) {
        var base = [
          '.' + dashize(key) + " {",
          "  fill: " + color + ";",
          "  background: " + color + ";",
          "}"
        ].join('\n');

        return [base].join('\n');
      }).join('\n');
    }

    var element = $('<style type="text/css"></style>').attr('id', stylesheetId).text(stylesheetContent());
    if (($('head').find('#' + stylesheetId).length)) {
      $('#' + stylesheetId).replaceWith(element);
    } else {
      $('head').append(element);
    }
  };

  $scope.updateStylesheet();

  function updateColors() {
    if ($scope.config.autoLighten) {
      $scope.lighten();
    }
    $scope.updateStylesheet();
  }

  $scope.$watch('config.autoLighten', updateColors, true);
  $scope.$watch('config.colors', updateColors, true);

}

SwitcherCtrl.$inject = ['$scope', 'DisplayProvider', 'ColorTools'];