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
    var itemId = (function() {
      var match = window.location.hash.match(/.*\/edit\/(.*)\?/);
      return (match && match[1]) ? match[1] : undefined;
    })();
    $routeProvider.when('/home', {templateUrl:'/web/partials/home', controller:HomeController});
    $routeProvider.when('/new', {redirectTo:'/new/blank'});
    $routeProvider.when('/new/:type', {templateUrl:'/web/partials/createItem', controller:CreateCtrl});
    $routeProvider.when('/new-v2/:type', {templateUrl:'/web/partials/createItem', controller:CreateV2Ctrl});
    $routeProvider.when('/edit/:itemId', {templateUrl:'/web/partials/editItem/' + itemId, controller:ItemController, reloadOnSearch: false});
    $routeProvider.when('/view/:itemId', {templateUrl:'/web/partials/viewItem', controller:ViewItemController, reloadOnSearch: false});
    $routeProvider.otherwise({redirectTo:'/home'});

}]);


