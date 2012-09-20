qtiDirectives.directive('csTabs', [
    '$timeout', function($timeout) {
      var controllerFn;
      controllerFn = function($scope, $element, $attrs) {
        var _this = this;
        
        if ($attrs['ngShow']) {
          $scope.$watch($attrs['ngShow'], function(newValue, oldValue) {
            return _this.tabsAreVisible = newValue;
          });
        } else {
          this.tabsAreVisible = true;
        }
        $scope.tabs = [];
        $scope.$watch('tabs.length', function(tabsL, oldL) {
          if (tabsL > 0 && tabsL < oldL) {
            if ($scope.tabs.indexOf($scope.selectedTab === -1)) {
              return $scope.selectTab($scope.tabs[Math.max($scope.selectedIdx - 1, 0)]);
            }
          }
        });
        $scope.selectTab = function(tab, $event) {
          var _i, _len, _ref, _tab;
          if (!(tab != null)) {
            return;
          }
          _ref = $scope.tabs;
          for (_i = 0, _len = _ref.length; _i < _len; _i++) {
            _tab = _ref[_i];
            _tab.selected(false);
          }
          $timeout(function() {
            return tab.selected(true);
          });
          $scope.selectedTab = tab;
          $scope.selectedIdx = $scope.tabs.indexOf(tab);
          if ($scope.onTabSelect != null) {
            $scope.onTabSelect(tab);
          }

          $event.preventDefault();
          $event.stopPropagation();
          return null;
        };
        $scope.changeTab = function(index) {
          try {
            return $scope.selectTab($scope.tabs[index]);
          } catch (e) {
            console.error("could not change tab, probably array out of bounds");
            throw e;
          }
        };
        this.addTab = function(tab, index) {
          $scope.tabs.push(tab);
          if ($scope.tabs.length === 1) {
            return $scope.selectTab(tab);
          }
        };
        this.removeTab = function(tab) {
          return $timeout(function() {
            return $scope.tabs.splice($scope.tabs.indexOf(tab, 1));
          });
        };
        this.nextTab = function() {
          var newIdx;
          newIdx = $scope.selectedIdx + 1;
          return $scope.changeTab(newIdx);
        };
        return this.previousTab = function() {
          var newIdx;
          newIdx = $scope.selectedIdx - 1;
          return $scope.changeTab(newIdx);
        };
      };
      return {
        restrict: 'E',
        transclude: true,
        controller: controllerFn,
        template: ['<div class="tab-panel">',
                    '<div class="tabs">',
                    '<ul><li ng-repeat="tab in tabs" >',
                    '<a href="" ng-class="{active: tab.selected()}" ng-click="selectTab(tab, $event)">{{tab.title}}</a>',
                    '</li></ul></div><div class="tab-box" ng-transclude></div></div>'].join('')

      };
    }
  ]);

qtiDirectives.directive('csTab', [
    function() {
      var linkFn, nextTab;
      nextTab = 0;
      linkFn = function(scope, elm, attrs, container) {
        var tab;
        tab = {
          title: attrs['title'],
          selected: function(newVal) {
            if (newVal == null) {
              return scope.selected;
            }
            return scope.selected = newVal;
          }
        };
        container.addTab(tab);
        return scope.$on('$destroy', function() {
          return container.removeTab(tab);
        });
      };
      return {
        restrict: 'E',
        transclude: true,
        require: '^csTabs',
        link: linkFn,
        scope: true,
        template: "<div class=\"paragraph-style\" ng-class=\"{active:selected}\" ng-show=\"selected\" ng-transclude></div>"
      };
    }
  ]);
