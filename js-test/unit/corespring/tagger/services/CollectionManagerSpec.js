describe('ItemService', function () {
  'use strict';

  var MockUserInfo = {};

  var collections = [
    {"id": "4ff2e56fe4b077b9e3168a05", "name": "CoreSpring Mathematics", itemCount: 0},
    {"id": "505777f5e4b05f7845735bc1", "name": "Beta Items", itemCount: 0},
    {"id": "50a22ccc300479fa2a5a66ac", "name": "default", itemCount: 0},
    {"id": "51baf73da196d2f175140218", "name": "Items from Production", itemCount: 0}
  ];

  MockUserInfo.org = {
    "id": "502404dd0364dc35bb39339c",
    "name": "Organization A",
    "path": ["502404dd0364dc35bb39339c"],
    "collections": [
      {"collectionId": "51baf73da196d2f175140218", "name": "Items from Production", "permission": "write"},
      {"collectionId": "50a22ccc300479fa2a5a66ac", "name": "default", "permission": "write"},
      {"collectionId": "505777f5e4b05f7845735bc1", "name": "Beta Items", "permission": "read"}
    ]
  };

  var prepareBackend = function ($backend) {

    var urls = [
      {method: 'GET', url: "/api/v1/collections", response: collections}
    ];

    for (var i = 0; i < urls.length; i++) {
      var definition = urls[i];
      $backend.when(definition.method, definition.url).respond(200, definition.response);
    }
  };


  beforeEach(function () {
    module(function ($provide) {
      $provide.value('UserInfo', MockUserInfo);
    });
  });

  var scope, manager, $httpBackend, at;

  beforeEach(module('tagger.services'));

  beforeEach(inject(function (_$httpBackend_, $rootScope, CollectionManager) {

    $httpBackend = _$httpBackend_;
    prepareBackend($httpBackend);
    scope = $rootScope.$new();

    try {
      manager = CollectionManager;
    } catch (e) {
      throw("Error with the service: " + e);
    }
  }));

  describe("inits", function () {
    it("is inited correctly", function () {
      expect(manager).not.toBeNull();
      expect(manager).not.toBeUndefined();
    });

    it("loads the initial data correctly", function () {

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

      manager.init();
      $httpBackend.flush();

      var simpleObject = function(c){
        return { id: c.id, name : c.name}
      };

      var orgCollections = _.map(manager.sortedCollections[0].collections, simpleObject);
      var publicCollections = _.map(manager.sortedCollections[1].collections, simpleObject);

      expect(orgCollections).toEqual(expected[0].collections);
      expect(publicCollections).toEqual(expected[1].collections);
    });
  });
});