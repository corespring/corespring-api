angular.module('tagger.services')
  .service('Modals',
    ['$rootScope',
    function($rootScope){


  $rootScope.modals = {
    clone: {
      show : false
    },
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
    confirmSave: {
    	show: false
    },
    commitFailedDueToConflict: {
      show: false
    },
    launchCodePreview: {
      show: false
    }
  };

  function Modals(){

    this.clone = function(item, collections, done){

      if(!item){
        throw new Error('no item defined');
      }

      if(!collections){
        throw new Error('no collections');
      }

      var cloneObj = $rootScope.modals.clone;
      cloneObj.show = true;
      cloneObj.item = item;
      cloneObj.collections = collections;

      var defaultCollection = _.find(collections, function(c){
        return c.name === 'Default';
      });

      cloneObj.collection = defaultCollection;

      cloneObj.agreed = false;

      cloneObj.done = function(cancelled, open){
        $rootScope.modals.clone.show = false;
        $rootScope.modals.clone.item = null;

        if(cloneObj.collection){
          done(cancelled, open, cloneObj.collection);
          cloneObj.collection = null;
        } else {
          throw new Error('no  object selected...');
        }
      };
    };

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

    this.confirmSave = function(done){
    	showModal('confirmSave', done);
    };

    this.commitFailedDueToConflict = function(done){
    	showModal('commitFailedDueToConflict', done);
    };

    this.launchCodePreview = function(done){
      showModal('launchCodePreview', done);
    };


    function showModal(name, done, item){
      $rootScope.modals[name].show = true;
      $rootScope.modals[name].item = item;
      $rootScope.modals[name].done = function(cancelled){
        $rootScope.modals[name].show = false;
        done(cancelled);
      };
    }
  }

  return new Modals();

}]);
