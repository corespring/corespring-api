'use strict';

describe('HomeController', function () {

  var MockUserInfo = {};
  // Mock dependencies
  var MockItemService = function () {
  };
  MockItemService.prototype.$save = jasmine.createSpy("Resource Save");
  MockItemService.createWorkflowObject = jasmine.createSpy("Create Workflow Object");

  var MockSearchService = function () {
  }
  MockSearchService.search = jasmine.createSpy("Search");

  beforeEach(function () {
    module(function ($provide) {
      $provide.value('ItemService', MockItemService);
      $provide.value('ServiceLookup', {});
      $provide.value('SupportingMaterial', {});
      $provide.value('SearchService', MockSearchService);
      $provide.value('CollectionManager', {
        init: function(){},
        addCollection: function(){},
        removeCollection: function(){},
        renameCollection: function(){},
        sortedCollections: []
      });
      $provide.value('Contributor', {
        query: function (data, result) {
          setTimeout(result, 0);
          return ["item1", "item2"];
        },
        get: function () {
        }
      });


    }, 'corespring-utils');
  });

  var scope, ctrl, $httpBackend;

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


  beforeEach(inject(function (_$httpBackend_, $rootScope, $controller) {
    $httpBackend = _$httpBackend_;
    prepareBackend($httpBackend);
    scope = $rootScope.$new();
    scope.search = function () {

    }

    try {
      ctrl = $controller(HomeController, {$scope: scope,Logger:{}});
    } catch (e) {
      throw("Error with the controller: " + e);
    }
  }));

  describe("inits", function () {

    it("is inited correctly", function () {

      expect(ctrl).not.toBeNull();
    });


    it("Search should invoke search service", function () {
      MockSearchService.search = jasmine.createSpy("Search").andCallFake(function (params, handler) {
        handler(["item"]);
      });
      scope.search();
      expect(MockSearchService.search).toHaveBeenCalled();
      expect(scope.items).toEqual(["item"]);
    });
  });

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

  });

});
