angular.module('tagger.services')
  .service('Modals',
    ['$rootScope',
    function($rootScope){


  $rootScope.modals = {
    publish: {
      show: false
    },
    edit: {
      show: false
    },
    'delete' : {
      show: false
    },
    saveConflictedDraft: {
    	show: false
    },
    commitFailedDueToConflict: {
      show: false
    }
  };

  function Modals(){

    this.publish = function(done){
      showModal('publish', done);
    };

    this.edit = function(done){
      showModal('edit', done);
    };

    this['delete'] = function(done){
      showModal('delete', done);
    };

    this.saveConflictedDraft = function(done){
    	showModal('saveConflictedDraft', done);
    };

    this.commitFailedDueToConflict = function(done){
    	showModal('commitFailedDueToConflict', done);
    };

    function showModal(name, done){
      $rootScope.modals[name].show = true;
      $rootScope.modals[name].done = function(cancelled){
        $rootScope.modals[name].show = false;
        done(cancelled);
      };
    }
  }

  return new Modals();

}]);
