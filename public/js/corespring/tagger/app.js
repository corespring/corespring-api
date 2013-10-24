'use strict';

// Declare app level module which depends on filters, and services
var taggerApp = angular.module('tagger',
    ['cs',
      'tagger.services',
      'angularBootstrap',
      'buttonToggle',
      'templates',
      'corespring-services',
      'corespring-utils',
      'corespring-directives',
      'ui',
      'tagger-context',
      'corespring-logger']);

taggerApp.
    config(['$routeProvider', function ($routeProvider) {
    $routeProvider.when('/home', {templateUrl:'/web/partials/home', controller:HomeController});
    $routeProvider.when('/new', {redirectTo:'/new/blank'});
    $routeProvider.when('/new/:type', {templateUrl:'/web/partials/createItem', controller:CreateCtrl});
    $routeProvider.when('/edit/:itemId', {templateUrl:'/web/partials/editItem', controller:ItemController, reloadOnSearch: false});
    $routeProvider.when('/view/:itemId', {templateUrl:'/web/partials/viewItem', controller:ViewItemController, reloadOnSearch: false});
    $routeProvider.otherwise({redirectTo:'/home'});

}]);


