'use strict';

describe('ItemService', function () {

  // Mock dependencies
  var MockItemService = function () {
  };
  MockItemService.prototype.$save = jasmine.createSpy("Resource Save");
  MockItemService.createWorkflowObject = jasmine.createSpy("Create Workflow Object");

  var MockSearchService = function () {
  }

  beforeEach(function () {
    module(function ($provide) {
      $provide.value('AccessToken', {token: 1});
    });
  });

  var scope, service, $httpBackend, at;

  beforeEach(module('tagger.services'));

  var prepareBackend = function ($backend) {

    var urls = [
      {method: 'PUT', url: /.*/, response: { ok: true }},
      {method: 'POST', url: /.*/, data: {}, response: { ok: true }}
    ];

    for (var i = 0; i < urls.length; i++) {
      var definition = urls[i];
      $backend.when(definition.method, definition.url).respond(200, definition.response);
    }
  };


  beforeEach(inject(function (_$httpBackend_, $rootScope, ItemService) {
    $httpBackend = _$httpBackend_;
    prepareBackend($httpBackend);
    scope = $rootScope.$new();


    try {
      service = ItemService;
    } catch (e) {
      throw("Error with the controller: " + e);
    }
  }));

  describe("inits", function () {
    it("is inited correctly", function () {
      expect(service).not.toBeNull();
      expect(service).not.toBeUndefined();

    });
  });

  describe("queries", function () {
    it("queries correctly", function () {
      expect(service).not.toBeNull();
      $httpBackend.expectGET('/api/v1/items/3').respond([
        {id: 0, name: 'Jack'},
        {id: 1, name: 'Bill'}
      ]);
      var items = service.query({id: '3'});
      $httpBackend.flush();
      expect(items[0].id).toEqual(0);
      expect(items[0].name).toEqual("Jack");
      expect(items[1].id).toEqual(1);
      expect(items[1].name).toEqual("Bill");
    });
  });

});
