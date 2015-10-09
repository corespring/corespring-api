angular.module('tagger.services')
  .service('Modals',
  ['$rootScope',
    function($rootScope) {

      var MODALS = ['publish', 'edit', 'delete', 'saveConflictedDraft', 'confirmSave', 'commitFailedDueToConflict'];

      $rootScope.modals = {};
      _.each(MODALS, function(m) {
        $rootScope.modals[m] = {show: false};
      });

      function Modals() {
        var self = this;
        _.each(MODALS, function(m) {
          self[m] = function(done) {
            showModal(m, done);
          }
        });

        function showModal(name, done) {
          $rootScope.modals[name].show = true;
          $rootScope.modals[name].done = function(cancelled) {
            $rootScope.modals[name].show = false;
            done(cancelled);
          };
        }
      }

      return new Modals();
    }
  ]
);
