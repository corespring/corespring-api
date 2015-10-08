(function(root) {
  'use strict';

  // Declare app level module which depends on filters, and services
  var taggerApp = angular.module('tagger',
    ['cs',
      'tagger.directives',
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
    config(['$routeProvider', function($rp) {

        function mk(path, templateUrl, controller, reloadOnSearch) {
          $rp.when(path, {
            templateUrl: templateUrl,
            controller: controller,
            reloadOnSearch: reloadOnSearch
          });
        }

        $rp.when('/new', {
          redirectTo: '/new/blank'
        });

        mk('/home', '/tagger/web/views/partials/tagger/home', tagger.HomeController);
        mk('/edit/draft/:itemId', '/tagger/web/views/partials/tagger/editDraft', tagger.EditDraftController, false);
        mk('/edit/:itemId', '/tagger/web/partials/editItem', ItemController, false);
        $rp.otherwise({
          redirectTo: '/home'
        });
    }]);
})(this);
