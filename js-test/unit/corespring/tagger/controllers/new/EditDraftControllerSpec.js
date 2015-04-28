describe('tagger.controllers.new.EditDraftController', function() {

  'use strict';

  var cmsService,
    ctrl,
    itemDraftService,
    itemService,
    location,
    routeParams,
    scope,
    v2ItemService;

  function MockItemService() {
    this.createWorkflowObject = jasmine.createSpy('createWorkflowObject');
    this.remove = jasmine.createSpy('remove');
  }

  beforeEach(module('tagger.services'));

  beforeEach(function() {
    location = {
      url: jasmine.createSpy('url')
    };

    v2ItemService = {
      clone: jasmine.createSpy('clone')
    };

    itemDraftService = {
      get: jasmine.createSpy('get'),
      getDraftsForOrg: jasmine.createSpy('getDraftsForOrg'),
      createUserDraft: jasmine.createSpy('createUserDraft').andCallFake(function(id, success, error) {
        success({
          id: 'd3'
        });
      }),
      publish: jasmine.createSpy('publish').andCallFake(function(id, success) {
        success({});
      })
    };

    routeParams = {itemId:123};

    module(function($provide) {
      $provide.value('$timeout', function(fn) {
        fn();
      });
      $provide.value('$location', location);
      $provide.value('$routeParams', routeParams);
      $provide.value('ItemService', MockItemService);
      $provide.value('ItemDraftService', itemDraftService);
      $provide.value('V2ItemService', v2ItemService);
      $provide.value('UserInfo', {
        userName: 'ed',
        org: '111'
      });
    }, 'corespring-utils');
  });

  beforeEach(inject(function(_$httpBackend_, $rootScope, $controller) {
    scope = $rootScope.$new();
    scope.search = function() {};

    ctrl = $controller(tagger.EditDraftController, {
      $scope: scope,
      Logger: {}
    });
  }));

  describe("init", function() {
    it("is initialised correctly", function() {
      expect(ctrl).not.toBeNull();
      expect(scope.hasChanges).toBe(false);
    });
  });

  describe("hasChanges", function() {
    it('flag gets set when itemChanged event occurs', function () {
      scope.onItemChanged();
      expect(scope.hasChanges).toBe(true);
    });
  });

  describe("showDevEditor", function() {
    it('sets devEditorVisible to true', function(){
      scope.showDevEditor();
      expect(scope.devEditorVisible).toBe(true);
    });
    it('sets v2Editor to url for dev editor', function(){
      scope.showDevEditor();
      expect(scope.v2Editor).toMatch("^/v2/player/dev-editor/123/index.html");
    });
    it('appends bypass-iframe-launch-mechanism=true', function(){
      scope.showDevEditor();
      expect(scope.v2Editor).toMatch("[?&]bypass-iframe-launch-mechanism=true");
    });
  });

  describe("showEditor", function() {
    it('sets devEditorVisible to false', function() {
      scope.showEditor();
      expect(scope.devEditorVisible).toBe(false);
    });
    it('sets v2Editor to url for dev editor', function(){
      scope.showEditor();
      expect(scope.v2Editor).toMatch("^/v2/player/editor/123/index.html");
    });
    it('appends bypass-iframe-launch-mechanism=true', function(){
      scope.showEditor();
      expect(scope.v2Editor).toMatch("[?&]bypass-iframe-launch-mechanism=true");
    });
  })



});