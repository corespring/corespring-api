angular.module('customColors.directives').directive('switcher', ['$timeout', function($timeout) {
  return {
    restrict: 'E',
    transclude: true,
    scope: {
      config: '='
    },
    link: function($scope, $element) {

      function dashize(string) {
        return string.replace(/([a-z])([A-Z])/g, '$1-$2').toLowerCase();
      }

      $scope.initSpectrum = function() {
        $element.find('.swatch').each(function(index, el) {
          $(el).spectrum({
            color: $(el).val(),
            preferredFormat: "hex",
            showInput: true,
            change: function() {
              $(el).trigger('input');
            }
          });
        });
      };

      $scope.setColors = function() {
        if ($scope.config.colors) {
          $scope.colorLoop = (function() {
            var keys = _.keys($scope.config.colors);
            var filtered = _.filter(keys, function(variable) {
              return !$scope.config.autoLighten ||
                (variable.toLowerCase().indexOf('dark') >= 0 || variable.toLowerCase().indexOf('accent') >= 0);
            });
            return _.map(filtered, function(variable) {
              return [dashize(variable), variable];
            });
          })();
          $timeout($scope.initSpectrum);
        }
      };

      $scope.reset = function() {
        $scope.$emit('settingsReset');
      };

      $scope.save = function() {
        $scope.$emit('settingsSave');
      };

      $scope.resetDefault = function() {
        $scope.$emit('settingsDefault');
      };

      $scope.setColors();
      $scope.$watch('config', $scope.setColors);


    },
    template: [
      '<div class="switcher">',
      '  <label>',
      '    Icon Set:',
      '    <select ng-model="config.iconSet">',
      '      <option value="emoji">Emoji</option>',
      '      <option value="check">Check</option>',
      '    </select>',
      '  </label>',
      '  <ul class="swatches">',
      '    <li ng-repeat="color in colorLoop">',
      '      <input class="swatch {{color[0]}}" type="text" ng-model="config.colors[color[1]]" />',
      '    </li>',
      '  </ul>',
      '  <div class="preview">',
      '    <h4>Preview</h4>',
      '    <ul class="svg-icons">',
      '      <li><svg-icon key="incorrect" shape="round" icon-set="{{config.iconSet}}"></svg-icon></li>',
      '      <li><svg-icon key="incorrect" shape="round" icon-set="{{config.iconSet}}" text="This is feedback"></svg-icon></li>',
      '      <li><svg-icon key="correct" shape="round" icon-set="{{config.iconSet}}"></svg-icon></li>',
      '      <li><svg-icon key="correct" shape="round" icon-set="{{config.iconSet}}" text="This is feedback"></svg-icon></li>',
      '      <li><svg-icon key="partially-correct" shape="round" icon-set="{{config.iconSet}}"></svg-icon></li>',
      '      <li><svg-icon key="partially-correct" shape="round" icon-set="{{config.iconSet}}" text="This is feedback"></svg-icon></li>',
      '      <li><svg-icon key="incorrect" shape="square" icon-set="{{config.iconSet}}"></svg-icon></li>',
      '      <li><svg-icon key="incorrect" shape="square" icon-set="{{config.iconSet}}" text="This is feedback"></svg-icon></li>',
      '      <li><svg-icon key="correct" shape="square" icon-set="{{config.iconSet}}"></svg-icon></li>',
      '      <li><svg-icon key="correct" shape="square" icon-set="{{config.iconSet}}" text="This is feedback"></svg-icon></li>',
      '      <li><svg-icon key="partially-correct" shape="square" icon-set="{{config.iconSet}}"></svg-icon></li>',
      '      <li><svg-icon key="partially-correct" shape="square" icon-set="{{config.iconSet}}" text="This is feedback"></svg-icon></li>',
      '    </ul>',
      '    <ul class="choice-icons">',
      '      <li><choice-icon class="animate-show icon" shape="radio" key="selected"></choice-icon></li>',
      '      <li><choice-icon class="animate-hide icon" shape="radio" key="muted"></choice-icon></li>',
      '      <li><choice-icon class="animate-show icon" shape="radio" key="correct"></choice-icon></li>',
      '      <li><choice-icon class="animate-show icon" shape="radio" key="incorrect"></choice-icon></li>',
      '    </ul>',
      '  </div>',
      '  <button class="btn" ng-click="save()">Save</button>',
      '  <button class="btn" ng-click="reset()">Reset to previous settings</button>',
      '  <button class="btn" ng-click="resetDefault()">Reset to CoreSpring defaults</button>',
      '</div>'
    ].join('\n')
  };
}]);