describe('ItemService', function () {
  'use strict';

  var MockUserInfo = {};

  var collections = [
    {"id": "4ff2e56fe4b077b9e3168a05", "name": "CoreSpring Mathematics", "isPublic": true ,itemCount: 0},
    {"id": "505777f5e4b05f7845735bc1", "name": "Beta Items", "isPublic": true, itemCount: 0},
    {"id": "50a22ccc300479fa2a5a66ac", "name": "default",  "ownerOrgId": "502404dd0364dc35bb39339c", itemCount: 0},
    {"id": "51baf73da196d2f175140218", "name": "Items from Production",  "ownerOrgId": "502404dd0364dc35bb39339c", itemCount: 0}
  ];

  MockUserInfo.org = {
    "id": "502404dd0364dc35bb39339c",
    "name": "Organization A",
    "path": ["502404dd0364dc35bb39339c"],
    "collections": [
      {"collectionId": "51baf73da196d2f175140218", "name": "Items from Production", "permission": "write", "enabled": true},
      {"collectionId": "50a22ccc300479fa2a5a66ac", "name": "default", "permission": "write", "enabled": true},
      {"collectionId": "505777f5e4b05f7845735bc1", "name": "Beta Items", "permission": "read", "enabled": true}
    ]
  };

  var prepareBackend = function ($backend) {

    var urls = [
      {method: 'GET', url: "/api/v1/collections", response: collections},
      {method: 'PUT', url: '/api/v2/collections/collectionId/share-with-org/orgId', response: {id: 'some-id'}},
      {method: 'GET', url: '/api/v2/organizations/with-shared-collection/collectionId', response: []}
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
    manager = CollectionManager;
  }));


  describe('shareCollection', function(){

    var onSuccess, onError;

    beforeEach(function(){
      onSuccess = jasmine.createSpy('onSuccess');
      onError = jasmine.createSpy('onError');
    });

    it('calls $http.put', function(){
       manager.shareCollection('collectionId', 'write', 'orgId', onSuccess, onError);
       var url = manager.urls.shareCollection('collectionId', 'orgId');
       $httpBackend.flush();
       expect(onSuccess).toHaveBeenCalledWith({id: 'some-id'});
    });
  });

  describe('getOrgsWithSharedCollection', function(){

    var onSuccess, onError;

    beforeEach(function(){
      onSuccess = jasmine.createSpy('onSuccess');
      onError = jasmine.createSpy('onError');
    });

    it('calls $http.get', function(){
       manager.getOrgsWithSharedCollection('collectionId', onSuccess, onError);
       var url = manager.urls.getOrgsWithSharedCollection('collectionId');
       $httpBackend.flush();
       expect(onSuccess).toHaveBeenCalledWith([]);
    });
  });

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
            {"id": "50a22ccc300479fa2a5a66ac", "name": "default"},
            {"id": "51baf73da196d2f175140218", "name": "Items from Production"}

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
        return { id: c.id, name : c.name};
      };

      var orgCollections = _.map(manager.sortedCollections[0].collections, simpleObject);
      var publicCollections = _.map(manager.sortedCollections[1].collections, simpleObject);

      expect(orgCollections).toEqual(expected[0].collections);
      expect(publicCollections).toEqual(expected[1].collections);
    });
  });
});