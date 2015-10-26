describe('tagger.controllers.new.EditDraftController', function() {

  var
    itemId,
    draftService,
    scope,
    mocks,
    jQueryFunctions,
    controller,
    rootScope,
    bindHandlers;

  beforeEach(module('tagger.services'));

  function mkController() {
    scope = rootScope.$new();
    scope.navigationHooks = {};
    controller(tagger.EditDraftController, {
      $scope: scope,
      ItemDraftService: mocks.itemDraftService,
      $routeParams: mocks.routeParams,
      $location: mocks.location,
      ItemService: mocks.ItemServiceConstructor,
      Modals: mocks.modals,
      Logger: mocks.logger,
      $window: mocks.window,
      $timeout: function(fn) {
        fn();
      }
    });
  }

  beforeEach(inject(function($rootScope, $controller) {

    mocks = {};

    mocks.editor = {
      forceSave: jasmine.createSpy('forceSave').andCallFake(function(success) {
        success(null);
      }),
      remove: jasmine.createSpy('remove')
    };

    window.org = window.org || {};
    org.corespring = org.corespring || {};
    org.corespring.players = org.corespring.players || {};
    org.corespring.players.DraftEditor = jasmine.createSpy('DraftEditor').andCallFake(
      function() {
        return mocks.editor;
      });

    mocks.location = {
      url: jasmine.createSpy('url'),
      path: jasmine.createSpy('path').andReturn({
        search: function() {}
      })
    };

    mocks.routeParams = {
      itemId: '123:0'
    };

    mocks.itemService = {

    };

    mocks.ItemServiceConstructor = jasmine.createSpy('new ItemService')
      .andCallFake(function() {
        return mocks.itemService;
      });

    mocks.itemDraftService = {
      get: jasmine.createSpy('get').andCallFake(function(opts, success) {
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
      deleteDraft: jasmine.createSpy('deleteDraft').andCallFake(function(id, success, error) {
        success();
      }),
      clone: jasmine.createSpy('clone').andCallFake(function(id, cb) {
        cb({
          itemId: id
        });
      })
    };

    mocks.modals = {
      cancelled: true,
      confirmSave: jasmine.createSpy('confirmSave').andCallFake(function(fn) {
        fn(mocks.modals.cancelled);
      }),
      saveConflictedDraft: jasmine.createSpy('saveConflictedDraft').andCallFake(function(fn) {
        fn(mocks.modals.cancelled);
      })
    };

    mocks.window = {
      confirm: jasmine.createSpy('confirm').andReturn(true)
    };

    mocks.logger = {
      info: function() {},
      debug: function() {},
      warn: function(){}
    };

    jQueryFunctions = {
      unbind: $.fn.unbind,
      bind: $.fn.bind
    };

    bindHandlers = {};
    $.fn.unbind = jasmine.createSpy('unbind');
    $.fn.bind = jasmine.createSpy('bind').andCallFake(function(key, handler) {
      bindHandlers[key] = bindHandlers[key] || [];
      bindHandlers[key].push(handler);
    });

    rootScope = $rootScope;
    controller = $controller;
    mkController();
  }));

  afterEach(function() {
    $.fn.unbind = jQueryFunctions.unbind;
    $.fn.bind = jQueryFunctions.bind;
  });

  describe('initialisation', function() {

    it('binds beforeunload', function () {
      expect($.fn.bind).toHaveBeenCalledWith('beforeunload', jasmine.any(Function));
    });

    describe("loading the initial item", function(){
      //there is no straightforward way to spy
      //on the scope itself, so we have to duplicate the tests
      //for initiallyDiscardAnyDraftAndLoadAFreshCopyOfTheItem here
      it('discards a draft', function(){
        mkController();
        expect(mocks.itemDraftService.deleteDraft).toHaveBeenCalled();
      });

      it('loads a draft', function(){
        mkController();
        expect(mocks.itemDraftService.get).toHaveBeenCalled();
      });

      it('loads a draft even if deleteDraft is failing', function(){
        mocks.itemDraftService.deleteDraft = jasmine.createSpy('deleteDraft').andCallFake(function(id, success, error) {
          error(1);
        });
        mkController();
        expect(mocks.itemDraftService.get).toHaveBeenCalled();
      });
    });

  });

  describe('initiallyDiscardAnyDraftAndLoadAFreshCopyOfTheItem', function(){

    it('discards a draft', function(){
      mkController();
      mocks.itemDraftService.deleteDraft.reset();
      scope.initiallyDiscardAnyDraftAndLoadAFreshCopyOfTheItem();
      expect(mocks.itemDraftService.deleteDraft).toHaveBeenCalled();
    });

    it('loads a draft', function(){
      mkController();
      mocks.itemDraftService.get.reset();
      scope.initiallyDiscardAnyDraftAndLoadAFreshCopyOfTheItem();
      expect(mocks.itemDraftService.get).toHaveBeenCalled();
    });

    it('loads a even if deleteDraft fails', function(){
      mkController();
      mocks.itemDraftService.deleteDraft = jasmine.createSpy('deleteDraft').andCallFake(function(id, success, error) {
        error(1);
      });
      mocks.itemDraftService.get.reset();
      scope.initiallyDiscardAnyDraftAndLoadAFreshCopyOfTheItem();
      expect(mocks.itemDraftService.get).toHaveBeenCalled();
    });
  });

  describe('beforeunload', function() {

    it('returns a message if hasChanges == true', function() {
      scope.hasChanges = true;
      expect(bindHandlers.beforeunload[0]()).toEqual(scope.unloadMessages.hasChanges);
    });

    it('returns a message if commitInProgress', function() {
      scope.commitInProgress = true;
      expect(bindHandlers.beforeunload[0]()).toEqual(scope.unloadMessages.commitInProgress);
    });

    it('returns undefined hasChanges == false', function() {
      scope.hasChanges = false;
      expect(bindHandlers.beforeunload[0]()).toBe(undefined);
    });
  });

  describe('navigationHooks.beforeUnload', function() {

    var callback;

    beforeEach(function() {
      callback = jasmine.createSpy('callback');
    });

    it('calls unbind', function() {
      scope.navigationHooks.beforeUnload(callback);
      expect($.fn.unbind).toHaveBeenCalledWith('beforeunload');
    });

    it('calls callback', function() {
      scope.hasChanges = false;
      scope.navigationHooks.beforeUnload(callback);
      expect(callback).toHaveBeenCalled();
    });

    it('calls modals.confirmSave', function() {
      scope.hasChanges = true;
      scope.navigationHooks.beforeUnload(callback);
      expect(mocks.modals.confirmSave).toHaveBeenCalledWith(jasmine.any(Function));
    });

    it('calls modals.confirmSave -> saveBackToItem if not cancelled', function() {
      spyOn(scope, 'saveBackToItem');
      scope.hasChanges = true;
      mocks.modals.cancelled = false;
      scope.navigationHooks.beforeUnload(callback);
      expect(scope.saveBackToItem).toHaveBeenCalledWith(jasmine.any(Function));
    });

    it('calls modals.confirmSave -> discardDraft if cancelled', function() {
      spyOn(scope, 'discardDraft');
      scope.hasChanges = true;
      mocks.modals.cancelled = true;
      scope.navigationHooks.beforeUnload(callback);
      expect(scope.discardDraft).toHaveBeenCalled();
    });
  });

  describe('discardDraft', function() {

    it('calls ItemDraftService.deleteDraft', function() {
      scope.discardDraft();
      expect(mocks.itemDraftService.deleteDraft)
        .toHaveBeenCalledWith(
          mocks.routeParams.itemId,
          jasmine.any(Function),
          jasmine.any(Function)
        );
    });
  });

  describe('confirmSaveBeforeLeaving', function() {
    it('calls $window.confirm', function() {
      scope.confirmSaveBeforeLeaving();
      expect(mocks.window.confirm).toHaveBeenCalled();
    });
  });

  describe('$routeChangeStart handler', function() {

    it('calls unbind', function() {
      scope.$emit('$routeChangeStart');
      expect($.fn.unbind).toHaveBeenCalledWith('beforeunload');
    });

    it('calls saveBackToItem', function() {
      spyOn(scope, 'saveBackToItem');
      scope.hasChanges = true;
      scope.$emit('$routeChangeStart');
      expect(scope.saveBackToItem).toHaveBeenCalled();
    });
  });

  describe('saveBackToItem', function() {
    it('if draftIsConflicted = false it doesn\'t call Modal.saveConflictedDraft', function() {
      scope.draftIsConflicted = false;
      scope.saveBackToItem();
      expect(mocks.modals.saveConflictedDraft).not.toHaveBeenCalledWith(jasmine.any(Function));
    });

    it('if draftIsConflicted it calls Modal.saveConflictedDraft', function() {
      scope.draftIsConflicted = true;
      scope.saveBackToItem();
      expect(mocks.modals.saveConflictedDraft).toHaveBeenCalledWith(jasmine.any(Function));
    });

    function callToServices(conflicted, cancelled, forced) {

      conflicted = conflicted || false;
      cancelled = cancelled === undefined ? true : cancelled;
      forced = forced === undefined ? false : forced;

      return function() {

        function label(l) {
          return 'conflicted: ' + conflicted + ', cancelled: ' + cancelled + ' forced: ' + forced + ' ' + l;
        }

        beforeEach(function() {
          scope.draftIsConflicted = conflicted;
          mocks.modals.saveConflictedDraft.andCallFake(function(fn) {
            fn(cancelled);
          });
          scope.saveBackToItem();
        });

        it(label('calls v2Editor.forceSave'), function() {
          if (cancelled) {
            expect(scope.v2Editor.forceSave).not.toHaveBeenCalled();
          } else {
            expect(scope.v2Editor.forceSave).toHaveBeenCalled();
          }
        });

        it(label('calls itemDraftService.commit'), function() {

          if (cancelled) {
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

  describe('clone', function() {
    it('calls itemDraftService.clone', function() {
      scope.clone();
      expect(mocks.itemDraftService.clone).toHaveBeenCalledWith(
        mocks.routeParams.itemId,
        jasmine.any(Function),
        jasmine.any(Function)
      );
    });
  });

  describe('loadDraftItem', function() {

    function load(getResult, expected) {

      return function() {
        beforeEach(function() {

          getResult = getResult || {
            draft: {},
            itemId: mocks.routeParams.itemId
          };

          var returnSuccess = typeof(getResult) === 'object';

          mocks.itemDraftService.get.andCallFake(function(opts, success, error) {
            if (returnSuccess) {
              success(getResult);
            } else {
              error(new Error('failed'), getResult);
            }
          });

          mkController();
        });

        it('sets showConflictError to false', function() {
          expect(scope.showConflictError).toBe(expected.showConflictError);
        });

        it('sets itemId', function() {
          expect(scope.itemId).toBe('123:0');
        });

        it('sets baseId', function() {
          expect(scope.baseId).toBe(expected.baseId);
        });

        it('sets version', function() {
          expect(scope.version).toBe(expected.version);
        });

        it('creates new editor', function() {
          expect(org.corespring.players.DraftEditor).toHaveBeenCalledWith(
            scope.containerClassName,
            jasmine.any(Object),
            jasmine.any(Function)
          );
        });
      };
    }

    describe('load no errors', load(undefined, {
      showConflictError: false,
      version: '0',
      baseId: '123'
    }));

    describe('load with 409', load(409, {
      showConflictError: true,
      version: undefined,
      baseId: undefined,
    }));

  });

  describe('discardAndLoadFreshCopy', function() {
    it('calls ItemDraftService.deleteDraft', function() {
      scope.discardAndLoadFreshCopy();
      expect(mocks.itemDraftService.deleteDraft).toHaveBeenCalled();
    });
    it('calls scope.loadDraftItem', function() {
      spyOn(scope, 'loadDraftItem');
      scope.discardAndLoadFreshCopy();
      expect(scope.loadDraftItem).toHaveBeenCalled();
    });
  });

  describe('showEditor', function() {

    function show(showEditorFn) {
      return function() {
        scope[showEditorFn]();
        expect(org.corespring.players.DraftEditor).toHaveBeenCalledWith(
          scope.containerClassName, {
            itemId: jasmine.any(String),
            draftName: jasmine.any(String),
            devEditor: showEditorFn === 'showDevEditor',
            onItemChanged: scope.onItemChanged,
            autosizeEnabled: false,
            hideSaveButton: true
          },
          jasmine.any(Function));
      };
    }

    it('calls new DraftEditor', show('showEditor'));
    it('calls new DraftEditor with devEditor: true', show('showDevEditor'));
  });


});