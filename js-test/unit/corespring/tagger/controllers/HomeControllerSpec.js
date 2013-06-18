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
      ctrl = $controller(HomeController, {$scope: scope});
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


    /*it("Correctly sorts the collections", function () {

      var collections = [
        {"id": "4ff2e56fe4b077b9e3168a05", "name": "CoreSpring Mathematics"},
        {"id": "505777f5e4b05f7845735bc1", "name": "Beta Items"},
        {"id": "50a22ccc300479fa2a5a66ac", "name": "default"},
        {"id": "51baf73da196d2f175140218", "name": "Items from Production"}
      ];

      var userOrg = {
          "id": "502404dd0364dc35bb39339c",
          "name": "Organization A",
          "path": ["502404dd0364dc35bb39339c"],
          "collections": [
            {"collectionId": "51baf73da196d2f175140218", "name": "Items from Production", "permission": "write"},
            {"collectionId": "50a22ccc300479fa2a5a66ac", "name": "default", "permission": "write"},
            {"collectionId": "505777f5e4b05f7845735bc1", "name": "Beta Items", "permission": "read"}
          ]};

      MockUserInfo.org = userOrg;

      var expected = [
        {
          "name": "Organization A",
          "collections": [
            {"id": "51baf73da196d2f175140218", "name": "Items from Production"},
            {"id": "50a22ccc300479fa2a5a66ac", "name": "default"}
          ]},
        {
          "name": "Public",
          "collections": [
            {"id": "4ff2e56fe4b077b9e3168a05", "name": "CoreSpring Mathematics"},
            {"id": "505777f5e4b05f7845735bc1", "name": "Beta Items"}
          ]
        }

      ];
      var allIds = _.pluck(userOrg.collections, "collectionId");
      expect(scope.createSortedCollection(collections, userOrg, allIds)).toEqual(expected);

    });
     */
  });

});
