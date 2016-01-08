describe('tagger.controllers.new.HomeController', function() {

  'use strict';

  function MockItemService() {
    this.createWorkflowObject = jasmine.createSpy('createWorkflowObject');
    this.get = jasmine.createSpy('get');
    this.remove = jasmine.createSpy('remove');
  }

  function MockModals() {
    this.publish = jasmine.createSpy('publish').andCallFake(function(cb) {
      cb(false);
    });
    this.edit = jasmine.createSpy('edit').andCallFake(function(cb) {
      cb(false);
    });
    this.delete = jasmine.createSpy('publish').andCallFake(function(cb) {
      cb(false);
    });
  }

  var cmsService,
    itemService,
    location,
    itemDraftService,
    modals,
    v2ItemService;

  beforeEach(module('tagger.services'));

  beforeEach(function() {
    location = {
      url: jasmine.createSpy('url')
    };

    v2ItemService = {
      clone: jasmine.createSpy('clone')
    };

    itemDraftService = {
      createUserDraft: jasmine.createSpy('createUserDraft').andCallFake(function(id, success, error) {
        success({
          id: 'd3'
        });
      }),
      deleteByItemId: jasmine.createSpy('deleteByItemId').andCallFake(function(id, success, error) {
        success()
      }),
      getDraftsForOrg: jasmine.createSpy('getDraftsForOrg'),
      publish: jasmine.createSpy('publish').andCallFake(function(id, success) {
        success({});
      })
    };

    itemService = new MockItemService();

    modals = new MockModals();

    module(function($provide) {
      $provide.value('$timeout', function(fn) {
        fn();
      });
      $provide.value('$location', location);
      $provide.value('ItemService', itemService);
      $provide.value('ItemDraftService', itemDraftService);
      $provide.value('V2ItemService', v2ItemService);
      $provide.value('UserInfo', {
        userName: 'ed',
        org: '111'
      });
      $provide.value('Modals', modals);

    }, 'corespring-utils');
  });

  var scope, ctrl;



  beforeEach(inject(function(_$httpBackend_, $rootScope, $controller) {
    scope = $rootScope.$new();

    ctrl = $controller(tagger.HomeController, {
      $scope: scope,
      Logger: {}
    });
  }));

  describe("init", function() {
    it("is initialised correctly", function() {
      expect(ctrl).not.toBeNull();
    });
  });

  function itSets(key, value) {
    it('sets' + key + ' to ' + value, function() {
      console.log('scope: ', scope);
      expect(scope[key]).toEqual(value);
    });
  }

  describe('launchCatalogView', function() {

    beforeEach(function() {
      scope.launchCatalogView.bind({
        item: {
          id: '1'
        }
      })();
    });

    itSets('showPopup', true);
    itSets('previewingId', '1');
    itSets('popupBg', 'extra-large-window');
  });

  describe('v2', function() {

    describe('publish', function() {

      var item;

      beforeEach(function() {
        item = {
          id: 'a',
          apiVersion: 2
        };
      });

      it('calls the underlying v2 publish if apiVersion is anything but 1', function() {
        item.apiVersion = 99;
        //spyOn(modals, 'publish');
        scope.publish(item);
        expect(modals.publish).toHaveBeenCalled();
      });
    });

    describe('clone', function() {

      beforeEach(function() {
        scope.items = [];
      });

      it('calls V2ItemService.clone', function() {

        v2ItemService.clone.andCallFake(function(obj, success) {
          success({
            id: 2,
            format: {
              apiVersion: 2
            }
          });
        });

        scope.v2.cloneItem({});

        expect(v2ItemService.clone).toHaveBeenCalled();
        expect(location.url).toHaveBeenCalledWith('/edit/draft/2');
      });
    });

    describe('delete item', function() {

      beforeEach(function() {
        scope.deleteItem({
          id: 123
        });
      });

      it('calls draftItemService.deleteByItemId', function() {
        expect(itemDraftService.deleteByItemId).toHaveBeenCalled();
      });

      it('calls itemService.remove', function() {
        expect(itemService.remove).toHaveBeenCalledWith({id:123}, jasmine.any(Function), jasmine.any(Function));
      });
    });
  });

  describe('v1', function() {
    describe('edit', function() {
      it("should launch the old editor", function() {
        scope.v1.edit({
          id: '123'
        });
        expect(location.url).toHaveBeenCalledWith('/edit/draft/123?devEditor=true');
      });
      it("should show edit modal when item is published", function() {
        scope.v1.edit({
          id: '123',
          published: true
        });
        expect(modals.edit).toHaveBeenCalled();
      });
    });
    describe('cloneItem', function() {
      var newItem;

      beforeEach(function() {
        itemService.get.andCallFake(function(obj, success) {
          newItem = {
            id: obj.id,
            clone: jasmine.createSpy('clone')
          };
          newItem.clone.andCallFake(function(success) {
            success({
              id: obj.id + "-clone"
            });
          });
          success(newItem);
        });
        scope.v1.cloneItem({
          id: 123
        });
      });

      it('calls ItemService.get', function() {
        expect(itemService.get).toHaveBeenCalled();
      });
      it('calls itemData.clone', function() {
        expect(newItem.clone).toHaveBeenCalled();
      });
      it("launches the old editor with the clone", function() {
        expect(location.url).toHaveBeenCalledWith('/edit/123-clone');
      });
    });
  });

});