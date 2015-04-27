
describe('tagger.HomeController', function () {

  'use strict';
  
  function MockItemService() {
   this.createWorkflowObject = jasmine.createSpy('createWorkflowObject');
   this.remove = jasmine.createSpy('remove');
  }

  var cmsService, 
  itemService, 
  location, 
  itemDraftService,
  v2ItemService;

  beforeEach(module('tagger.services'));

  beforeEach(function () {
    location = {
      url: jasmine.createSpy('url')
    };

    v2ItemService = {
      clone: jasmine.createSpy('clone')
    };

    itemDraftService = {
      getDraftsForOrg: jasmine.createSpy('getDraftsForOrg'),
      createUserDraft: jasmine.createSpy('createUserDraft').andCallFake(function(id, success,error){
        success({id: 'd3'});
      }),
      publish: jasmine.createSpy('publish').andCallFake(function(id, success){
        success({});
      })
    };

    itemService = new MockItemService();
    module(function ($provide) {
      $provide.value('$timeout', function(fn){fn();});
      $provide.value('$location',  location);
      $provide.value('ItemService', itemService);
      $provide.value('ItemDraftService', itemDraftService);
      $provide.value('V2ItemService', v2ItemService);
      $provide.value('UserInfo', {userName: 'ed', org: '111'});
    }, 'corespring-utils');
  });

  var scope, ctrl;



  beforeEach(inject(function (_$httpBackend_, $rootScope, $controller) {
    scope = $rootScope.$new();
    scope.search = function () {};

    ctrl = $controller(tagger.HomeController, {$scope: scope,Logger:{}});
  }));

  describe("inits", function () {
    it("is inited correctly", function () {
      expect(ctrl).not.toBeNull();
    });
  });

  function itSets(key, value){
    it('sets' + key + ' to ' + value, function(){
      console.log('scope: ', scope);
      expect(scope[key]).toEqual(value);
    });
  }

  describe('launchCatalogView', function(){

    beforeEach(function(){
      scope.launchCatalogView.bind({item: {id: '1'}})();
    });

    itSets('showPopup', true);
    itSets('previewingId', '1');
    itSets('popupBg', 'extra-large-window');
  });

  describe('v2', function(){

    describe('edit', function(){

      beforeEach(function(){
        scope.orgDrafts = [{id: 'd1', itemId: '1'}];
      });

    });

    describe('publish', function(){

      var item;


      beforeEach(function(){
        item = {id: 'a', format: {apiVersion: 2}};
        scope.orgDrafts = [{id: 'da', itemId: 'a'}];
        scope.publish(item);
      });
      
      // it('sets the publish flag', function(){
      //   expect(scope.showPublishNotification).toBe(true);
      // });
      
      // it('sets the item to be published', function(){
      //   expect(scope.itemToPublish).toEqual(item);
      // }); 

      // it('calls the underlying publish', function(){
      //   scope.publishConfirmed();
      //   expect(itemDraftService.publish).toHaveBeenCalled();
      // });
    });

    describe('clone', function(){

      beforeEach(function(){
        scope.items = [];
        scope.orgDrafts = [];
      });

      it('calls V2ItemService.clone', function(){

        v2ItemService.clone.andCallFake(function(obj, success){
          success({id: 2, format: { apiVersion: 2}});
        });

        scope.v2.cloneItem({});

        expect(v2ItemService.clone).toHaveBeenCalled();
        expect(location.url).toHaveBeenCalledWith('/edit/draft/2');
      });
    });

    describe('delete item', function(){

      beforeEach(function(){
        scope.deleteItem({});
      });

      // it('sets delete flag', function(){
      //   scope.showConfirmDestroyModal = true;
      // });

      // it('sets delete item to delete', function(){
      //   expect(scope.itemToDelete).toEqual({});
      // });

      // it('calls ItemService.remove', function(){
      //   scope.deleteConfirmed();
      //   expect(itemService.remove).toHaveBeenCalled();
      // });
    });
  });



  

    /*
    it("Search should invoke search service", function () {
      MockSearchService.search = jasmine.createSpy("Search").andCallFake(function (params, handler) {
        handler(["item"]);
      });
      scope.search();
      expect(MockSearchService.search).toHaveBeenCalled();
      expect(scope.items).toEqual(["item"]);
    });*/

  /*

  describe("getSelectedTitle", function(){

    it("should not remove 0 from 10", function () {
      var result = scope.getSelectedTitle([{key:"10"}]);
      expect(result).toEqual("10");
    });

    it("should remove leading 0", function () {
      var result = scope.getSelectedTitle([{key:"02"}]);
      expect(result).toEqual("2");
    });

    it("should remove multiple leading 0", function () {
      var result = scope.getSelectedTitle([{key:"0002"}]);
      expect(result).toEqual("2");
    });

    it("should remove leading 0 in multiple items", function () {
      var result = scope.getSelectedTitle([{key:"02"},{key:"03"}]);
      expect(result).toEqual("2, 3");
    });


    it("should leave non numeric values alone", function () {
      var result = scope.getSelectedTitle([{key:"abc"}]);
      expect(result).toEqual("abc");
    });

  }); */

});
