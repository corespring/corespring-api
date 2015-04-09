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

        mk('/home', '/web/views/partials/tagger/home', tagger.HomeController);
        mk('/new/:type', '/web/views/partials/tagger/createItem', tagger.CreateCtrl);
        mk('/edit/draft/:draftId', '/web/views/partials/tagger/editDraft', tagger.EditDraftController, false);
        mk('/edit/:itemId', '/web/views/partials/tagger/editItem', tagger.ItemController, false);
        mk('/view/:itemId', '/web/views/partials/tagger/viewItem', tagger.ViewItemController, false);
        /* jshint ignore:start */
        mk('/old/edit/:itemId', '/web/partials/editItem', ItemController, false);
        /* jshint ignore:end */
        $rp.otherwise({
          redirectTo: '/home'
        });
    }]);
})(this);