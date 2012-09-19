qtiDirectives.directive("tabs", function() {
        var tabTemplate = [
                        '<div class="tab-panel">',
                        '<div class="tabs"><ul>',
                            '<li ng:repeat="item in items" obj="{{item}}"> ',
                            '<a href="#tab{{$index}}" ng-class="{active: $index==selectedIndex}" ng-click="onClick($index)">{{item.tabTitle}}</a>',
                            '</li>',
                        '</div>',
                        '<div ng:repeat="item in items" class="tab-box" id="tab{{$index}}" ng-show="selectedIndex == $index">',
                            '<div class="paragraph-style">',
                            '{{item.content}}',
                        '</div></div></div>',
                        ''].join('');


    return {
        restrict: 'E',
        replace: true,
        scope: true,
        transclude: false,
        controller: function($scope, $element, $attrs) {
            var scope = $scope;
            scope.selectedIndex = 0;

        },
        compile: function(tElement, tAttrs, transclude) {
            var tabs = [];
            var elements = [];
            console.log('compile bla function');
            var tabElements = angular.element(tElement).find("tab");
            for (var i = 0; i < tabElements.length; i++)  {
                var elem = angular.element(tabElements[i]);
                var tabTitle = elem.attr('name');
                tabs.push({content: elem.html(), tabTitle: tabTitle});
                elements.push(tabElements[i]);
            }

            // now modify the DOM
            tElement.html(tabTemplate);

            // linking function
            return function(scope, element, attrs) {
                scope.items = tabs;
                scope.onClick = function(idx) {
                    scope.selectedIndex = idx;
                }
            };
        }
    }
});
