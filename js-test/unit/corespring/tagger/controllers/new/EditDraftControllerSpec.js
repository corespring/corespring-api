describe('tagger.controllers.new.EditDraftController', function() {

  'use strict';

  var itemId,
  draftService,
  scope,
  mocks,
  jQueryFunctions;

  
  beforeEach(module('tagger.services'));

  beforeEach(function(){

  });
  
  beforeEach(inject(function($rootScope, $controller) { 

    mocks = {};

    mocks.editor = {
      forceSave: jasmine.createSpy('forceSave').andCallFake(function(success){
        success(null);
      }),
      remove: jasmine.createSpy('remove')
    };

    window.org = window.org || {};
    org.corespring = org.corespring || {};
    org.corespring.players = org.corespring.players || {};
    org.corespring.players.DraftEditor = jasmine.createSpy('DraftEditor').andCallFake(
      function(){
        return mocks.editor;
      });

    mocks.location = {
      url: jasmine.createSpy('url'),
      path: jasmine.createSpy('path').andReturn({
        search: function(){} 
      })
    };

    mocks.routeParams = {
      itemId: '123:0'
    };

    mocks.itemService = {

    };

    mocks.ItemServiceConstructor = jasmine.createSpy('new ItemService')
      .andCallFake(function(){
        return mocks.itemService;
      });
    
    mocks.itemDraftService = {
      get: jasmine.createSpy('get').andCallFake(function(opts, success){
        success({
          itemId: mocks.routeParams.itemId,
          user: 'ed'
        });
      }),
      getDraftsForOrg: jasmine.createSpy('getDraftsForOrg'),
      createUserDraft: jasmine.createSpy('createUserDraft').andCallFake(function(id, success, error) {
        success({
          id: 'd3'
        });
      }),
      publish: jasmine.createSpy('publish').andCallFake(function(id, success) {
        success({});
      }),
      commit: jasmine.createSpy('commit').andCallFake(function(id, force, success) {
        success({});
      }),
      deleteDraft : jasmine.createSpy('deleteDraft'),
      clone: jasmine.createSpy('clone').andCallFake(function(id, cb){
        cb({itemId: id});
      })
    };

    mocks.modals = {
      cancelled: true,
      saveConflictedDraft: jasmine.createSpy('saveConflictedDraft').andCallFake(function(fn){
        fn(mocks.modals.cancelled);
      })
    };

    mocks.window = {
      confirm: jasmine.createSpy('confirm').andReturn(true)
    };

    mocks.logger = {
      info: function(){}
    };

    jQueryFunctions = {
      unbind: $.fn.unbind
    };

    $.fn.unbind = jasmine.createSpy('unbind');

    scope = $rootScope.$new();
    scope.navigationHooks = {};
    $controller(tagger.EditDraftController, {
      $scope: scope,
      ItemDraftService: mocks.itemDraftService,
      $routeParams: mocks.routeParams,
      $location: mocks.location,
      ItemService: mocks.ItemServiceConstructor,
      Modals: mocks.modals,
      Logger: mocks.logger,
      $window: mocks.window,
      $timeout: function(fn){ fn(); }
    });
  }));

  afterEach(function(){
    $.fn.unbind = jQueryFunctions.unbind;
  });

  describe('discardDraft', function(){

    it('calls ItemDraftService.deleteDraft', function(){
      scope.discardDraft();
      expect(mocks.itemDraftService.deleteDraft)
        .toHaveBeenCalledWith(
          mocks.routeParams.itemId, 
          jasmine.any(Function),
          jasmine.any(Function)
          );
    });
  });

  describe('confirmSaveBeforeLeaving', function(){
    it('calls $window.confirm', function(){
      scope.confirmSaveBeforeLeaving();
      expect(mocks.window.confirm).toHaveBeenCalled();
    });
  });

  describe('$routeChangeStart handler', function(){

    it('calls unbind', function(){
      scope.$emit('$routeChangeStart');
      expect($.fn.unbind).toHaveBeenCalledWith('beforeunload');
    });

    it('calls saveBackToItem', function(){
      spyOn(scope, 'saveBackToItem');
      scope.hasChanges = true;
      scope.$emit('$routeChangeStart');
      expect(scope.saveBackToItem).toHaveBeenCalled();
    });
  });

  describe('saveBackToItem', function(){
    it('if draftIsConflicted = false it doesn\'t call Modal.saveConflictedDraft', function(){
      scope.draftIsConflicted = false;
      scope.saveBackToItem();
      expect(mocks.modals.saveConflictedDraft).not.toHaveBeenCalledWith(jasmine.any(Function));
    });

    it('if draftIsConflicted it calls Modal.saveConflictedDraft', function(){
      scope.draftIsConflicted = true;
      scope.saveBackToItem();
      expect(mocks.modals.saveConflictedDraft).toHaveBeenCalledWith(jasmine.any(Function));
    });

    function callToServices(conflicted, cancelled, forced){

      conflicted = conflicted || false;
      cancelled = cancelled === undefined ? true : cancelled;
      forced = forced === undefined ? false : forced;

      return function(){

          function label(l){
            return 'conflicted: ' + conflicted + ', cancelled: ' + cancelled + ' forced: ' + forced + ' ' + l;
          }

          beforeEach(function(){
            scope.draftIsConflicted = conflicted;
            mocks.modals.saveConflictedDraft.andCallFake(function(fn){
              fn(cancelled);
            });
            scope.saveBackToItem();
          });
          
          it(label('calls v2Editor.forceSave'), function(){
            if(cancelled){
              expect(scope.v2Editor.forceSave).not.toHaveBeenCalled();
            } else {
              expect(scope.v2Editor.forceSave).toHaveBeenCalled();
            }
          });
          
          it(label('calls itemDraftService.commit'), function(){

            if(cancelled){
              expect(mocks.itemDraftService.commit).not.toHaveBeenCalledWith();
            } else {
              expect(mocks.itemDraftService.commit).toHaveBeenCalledWith(
                mocks.routeParams.itemId, 
                forced, 
                jasmine.any(Function), 
                jasmine.any(Function));
            }
          });
      };
    }

    describe('call to services - not conflicted => force is false', callToServices(false, false, false));
    describe('call to services - conflicted => force is true', callToServices(true, false, true));
    describe('call to services - conflicted + cancelled - no calls to services', callToServices(true, true, true));
  });

  describe('clone', function(){
    it('calls itemDraftService.clone', function(){
      scope.clone();
      expect(mocks.itemDraftService.clone).toHaveBeenCalledWith(
        mocks.routeParams.itemId, 
        jasmine.any(Function),
        jasmine.any(Function)
        );
    });
  });

  describe('loadDraftItem', function(){

    it('creates new editor', function(){
      scope.loadDraftItem(true);
      expect(org.corespring.players.DraftEditor).toHaveBeenCalledWith(
        '.draft-editor-holder', 
        jasmine.any(Object), 
        jasmine.any(Function)
        );
    });
  });


});