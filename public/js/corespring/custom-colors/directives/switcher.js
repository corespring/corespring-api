angular.module('customColors.directives').directive('switcher', ['$timeout', function($timeout) {
  return {
    restrict: 'E',
    transclude: true,
    scope: {
      config: '='
    },
    link: function($scope, $element) {

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
      '  <p>',
      '    Organizations may select icons and color schemes to align with branding. Click Save to apply the changes.',
      '  </p>',
      '  <label>',
      '    <span class="header icon-set-header">Icon Set</span>',
      '    <select ng-model="config.iconSet">',
      '      <option value="emoji">Emoji</option>',
      '      <option value="check">Check</option>',
      '    </select>',
      '  </label>',
      '  <span class="header">Color Scheme</span>',
      '  <div class="resets">',
      '    <button class="btn" ng-click="reset()">Reset to previous settings</button></br>',
      '    <button class="btn" ng-click="resetDefault()">Reset to CoreSpring defaults</button>',
      '  </div>',
      '  <div class="swatches">',
      '    <div>',
      '      <div class="icon-holder">',
      '        <svg-icon key="correct" shape="round" icon-set="{{config.iconSet}}"></svg-icon>',
      '        <span>Correct</span>',
      '      </div>',
      '      <div class="background">',
      '        <label>',
      '          Main',
      '          <input class="swatch" type="text" ng-model="config.colors[\'correct-background\']" />',
      '        </label>',
      '      </div>',
      '      <div class="foreground">',
      '        <label>',
      '          Accent',
      '          <input class="swatch" type="text"ng-model="config.colors[\'correct-foreground\']" />',
      '        </label>',
      '      </div>',
      '    </div>',
      '    <div>',
      '      <div class="icon-holder">',
      '        <svg-icon key="incorrect" shape="round" icon-set="{{config.iconSet}}"></svg-icon>',
      '        <span>Incorrect</span>',
      '      </div>',
      '      <div class="background">',
      '        <label>',
      '          Main',
      '          <input class="swatch" type="text" ng-model="config.colors[\'incorrect-background\']" />',
      '        </label>',
      '      </div>',
      '      <div class="foreground">',
      '        <label>',
      '          Accent',
      '          <input class="swatch" type="text"ng-model="config.colors[\'incorrect-foreground\']" />',
      '        </label>',
      '      </div>',
      '    </div>',
      '    <div>',
      '      <div class="icon-holder">',
      '        <svg-icon key="partially-correct" shape="round" icon-set="{{config.iconSet}}"></svg-icon>',
      '        <span>Partially</span>',
      '      </div>',
      '      <div class="background">',
      '        <label>',
      '          Main',
      '          <input class="swatch" type="text" ng-model="config.colors[\'partially-correct-background\']" />',
      '        </label>',
      '      </div>',
      '      <div class="foreground">',
      '        <label>',
      '          Accent',
      '          <input class="swatch" type="text"ng-model="config.colors[\'partially-correct-foreground\']" />',
      '        </label>',
      '      </div>',
      '    </div>',
      '  </div>',
      '  <button class="btn save-btn" ng-click="save()">Save</button>',
      '</div>'
    ].join('\n')
  };
}]);