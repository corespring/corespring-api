'use strict';


// Declare app level module which depends on filters, and services
var taggerApp = angular.module('tagger', ['cs', 'tagger.filters', 'tagger.services', 'tagger.directives'
    , 'angularBootstrap', 'buttonToggle', 'templates', 'ui']);

taggerApp.
    config(['$routeProvider', function ($routeProvider) {
    $routeProvider.when('/item-collection', {templateUrl:'/web/partials/itemCollection', controller:ItemCollectionController});
    $routeProvider.when('/edit/:itemId', {templateUrl:'/web/partials/editMetadata', controller:EditCtrl, reloadOnSearch: false});
    $routeProvider.when('/new', {redirectTo:'/new/blank'});
    $routeProvider.when('/new/:type', {templateUrl:'/web/partials/createItem', controller:CreateCtrl});
    $routeProvider.when('/collections', {templateUrl:'old.partials/collections.html', controller:CollectionsCtrl});
    $routeProvider.otherwise({redirectTo:'/item-collection'});
}]);


// module for showing search count and paging through search results in editor
taggerApp.directive('resultsPager', function (searchService) {
    var resultsPagerDirective = {};

    resultsPagerDirective.controller = function ($scope, $element, $attrs, searchService, $location) {
        $scope.next = function () {
            changePage(1);
        };
        $scope.previous = function () {
            changePage(-1);
        };
        $scope.updatePagerText = function () {

            if (
                searchService.itemDataCollection === undefined ||
                searchService.itemDataCollection.length == 0 ||
                typeof(searchService.itemDataCollection.indexOf) !== "function") {
                $scope.pagerText = "";
                return;
            }

            var currentIndex = searchService.itemDataCollection.indexOf(searchService.currentItem);
            var currentIndexText = "";
            if (currentIndex != -1) {
                currentIndexText = (currentIndex + 1) + " of ";
            }
            $scope.pagerText = currentIndexText + $scope.resultCount + " results";
        };

        function changePage(number) {

            var currentIndex = searchService.itemDataCollection.indexOf(searchService.currentItem);
            var proposedIndex = currentIndex + number;

            if (proposedIndex < 0 || proposedIndex > (searchService.resultCount - 1)) {
                console.debug("can't nav to nothing");
                return;
            }

            var nextItem = searchService.itemDataCollection[proposedIndex];
            if (nextItem) {
                gotoNextItem(nextItem);
            } else {
                searchService.loadMore(function () {
                    var nextItem = searchService.itemDataCollection[currentIndex + number];
                    gotoNextItem(nextItem);
                });
            }
        }

        function gotoNextItem(nextItem) {
            $location.path('/edit/' + nextItem._id.$oid);
            searchService.currentItem = nextItem;
            $scope.updatePagerText();
        }

    };

    resultsPagerDirective.link = function ($scope, element, attr, ctrl) {
        console.log("in resultsPagerDirective");
        // broadcast by search service
        $scope.$on('onSearchCountComplete', function (evt, resultCount) {
            $scope.resultCount = resultCount;
            $scope.updatePagerText();
        });
        $scope.$on('onListViewOpened', function (evt, resultCount) {
            console.log('received onListViewOpened');
            $scope.editViewOpen = false;
        });
        $scope.$on('onEditViewOpened', function (evt, resultCount) {
            console.log('received onEditViewOpened');
            $scope.updatePagerText();
            $scope.editViewOpen = true;
        });

        $scope.$on('createNewItem', function (evt) {
            searchService.resetDataCollection();
            $scope.updatePagerText();
        });

        $scope.$on('itemDeleted', function(evt){
            searchService.resetDataCollection();
            $scope.updatePagerText();
        })

    };
    return resultsPagerDirective;
});


// module for showing search count and paging through search results in editor
taggerApp.directive('networkProgress', function (searchService) {
    var networkProgressDirective = {};


    networkProgressDirective.link = function ($scope, element, attr, ctrl) {
        $scope.isLoading = false;

        $scope.$on('onNetworkLoading', function (evt, resultCount) {

            $scope.isLoading = true;
        });

        $scope.$on('onNetworkComplete', function (evt, resultCount) {
            $scope.isLoading = false;
        });

    };
    return networkProgressDirective;
});


// module for toggling a hidden pansl
taggerApp.directive('togglePanel', function ($rootScope) {
    return function (scope, element, attr) {

        var expanderPanel = attr.expanderPanel;
        var mainPanel = attr.mainPanel;
        var expandButton = attr.expandButton;
        var collapseButton = attr.collapseButton;

        var expandedSidePanelClass = (attr.togglePanelActiveClass || "toggle-panel-expanded-side-panel");
        var expandedMainPanelClass = (attr.expandedMainPanelClass || "toggle-panel-expanded-main-panel")

        var callback = attr.togglePanelCallback;

        scope.$watch('panelOpen', function (value) {

            var toggleButtons = function (panelOpen) {
                if (panelOpen) {
                    angular.element('#' + expandButton).addClass('disabled');
                    angular.element('#' + collapseButton).removeClass('disabled');
                }
                else {
                    angular.element('#' + expandButton).removeClass('disabled');
                    angular.element('#' + collapseButton).addClass('disabled');
                }
            }

            if (scope.panelOpen === undefined) return; // initial state

            toggleButtons(scope.panelOpen);

            if (scope.panelOpen) {
                $('#' + expanderPanel).removeClass('hide');
                $('#' + expanderPanel).addClass(expandedSidePanelClass);
                ;
                $('#' + mainPanel).addClass(expandedMainPanelClass);
            } else {
                $('#' + expanderPanel).addClass('hide');
                $('#' + expanderPanel).removeClass(expandedSidePanelClass);
                $('#' + mainPanel).removeClass(expandedMainPanelClass);
            }
            scope.$root.$broadcast("panelOpen", scope.panelOpen);
        });


        scope.expand = function () {
            scope.$apply(scope.panelOpen = true);
        };

        scope.collapse = function () {
            scope.$apply(scope.panelOpen = false);
        };


        var expandButtonElem = angular.element('#' + expandButton);
        expandButtonElem.bind('click', scope.expand);
        //angular.element('#' + expandButton).bind('click', scope.expand);
        angular.element('#' + collapseButton).bind('click', scope.collapse);

    };
});


// module for using css-toggle buttons instead of checkboxes
// toggles the class named in button-toggle element if value is checked
angular.module('buttonToggle', []).directive('buttonToggle', function () {
    return {
        restrict:'A',
        require:'ngModel',
        link:function ($scope, element, attr, ctrl) {
            var classToToggle = attr.buttonToggle;
            element.bind('click', function () {
                var checked = ctrl.$viewValue;
                $scope.$apply(function (scope) {
                    ctrl.$setViewValue(!checked);
                });
            });

            $scope.$watch(attr.ngModel, function (newValue, oldValue) {
                newValue ? element.addClass(classToToggle) : element.removeClass(classToToggle);
            });
        }
    };
});




