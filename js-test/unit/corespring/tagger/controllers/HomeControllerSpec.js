
describe('tagger.HomeController', function () {

  'use strict';
  
  function MockItemService() {
   this.createWorkflowObject = jasmine.createSpy('createWorkflowObject');
  }

  var cmsService, mockItemService, location, itemDraftService;

  beforeEach(function () {
    cmsService = {
      getDraftsForOrg: jasmine.createSpy('getDraftsForOrg')
    };

    location = {
      url: jasmine.createSpy('url')
    };


    itemDraftService = {
      createUserDraft: jasmine.createSpy('createUserDraft').andCallFake(function(id, success,error){
        success({id: 'd3'});
      })
    };

    mockItemService = new MockItemService();
    module(function ($provide) {
      $provide.value('$timeout', function(fn){fn();});
      $provide.value('$location',  location);
      $provide.value('ItemService', mockItemService);
      $provide.value('CmsService', cmsService);
      $provide.value('ItemDraftService', itemDraftService);
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

      it('sets the location to the draft', function(){
        scope.v2.edit({id: '1'});
        expect(location.url).toHaveBeenCalledWith('/edit/draft/d1');
      });
      
      it('sets the location to the draft', function(){
        scope.v2.edit({id: '2'});
        expect(itemDraftService.createUserDraft).toHaveBeenCalled();
        expect(location.url).toHaveBeenCalledWith('/edit/draft/d3');
      });
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
