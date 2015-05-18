/* global _ */
'use strict';

describe('ItemController should', function () {

  var routeParams, rootScope, scope, ctrl, $httpBackend, location, http;

  beforeEach(function () {
    module(function ($provide) {
      $provide.value('AccessToken', {token: "1"});
    });
  });
  beforeEach(module('tagger.services'));

  var prepareBackend = function ($backend) {

    var urls = [
      {method: 'GET', url: "/api/v1/metadata/item", response: {ok:true}},
      {method: 'PUT', url: /.*/, response: { ok: true }},
      {method: 'POST', url: '/api/v1/items/itemId/increment', data: {}, response: {id: 'itemId2', version: {rev: 2}}},
      //{method: 'POST', url: /.*/, data: {}, response: { ok: true }},
      {method: 'GET', url: "/api/v1/collections", response: {}},
      {method: 'GET', url: "/api/v1/items", response: {}},
      {method: 'GET', url: "/api/v1/items/:itemId", response: {id: "itemId",version: {rev:1}}},
      {method: 'GET', url: "/assets/web/standards_tree.json", response: {}},
      {method: 'GET', url: "/api/v1/field_values/domain", response: {}}
    ];

    for (var i = 0; i < urls.length; i++) {
      var definition = urls[i];
      $backend.when(definition.method, definition.url).respond(200, definition.response);
    }
  };


  beforeEach(inject(function (_$httpBackend_, $rootScope, $controller, $location, $http) {
    $httpBackend = _$httpBackend_;
    prepareBackend($httpBackend);
    rootScope = $rootScope;
    scope = $rootScope.$new();
    routeParams = {};
    location = $location;
    http = $http;

    try {
      ctrl = $controller(ItemController, {$scope: scope, $routeParams: routeParams, $location: location, Logger: {}});
    } catch (e) {
      throw("Error with the controller: " + e);
    }
  }));


  it('init correctly', inject(function () {
    expect(ctrl).not.toBeNull();
  }));

  it('calculates pValue as string', function () {
    expect(scope.getPValueAsString(0)).toBe("");
    expect(scope.getPValueAsString(1)).toBe("Very Hard");
    expect(scope.getPValueAsString(19)).toBe("Very Hard");
    expect(scope.getPValueAsString(20)).toBe("Very Hard");
    expect(scope.getPValueAsString(21)).toBe("Moderately Hard");
    expect(scope.getPValueAsString(40)).toBe("Moderately Hard");
    expect(scope.getPValueAsString(41)).toBe("Moderate");
    expect(scope.getPValueAsString(60)).toBe("Moderate");
    expect(scope.getPValueAsString(61)).toBe("Easy");
    expect(scope.getPValueAsString(80)).toBe("Easy");
    expect(scope.getPValueAsString(81)).toBe("Very Easy");
    expect(scope.getPValueAsString(99)).toBe("Very Easy");
    expect(scope.getPValueAsString(100)).toBe("Very Easy");
  });

  it('creates mongo queries for standards', function () {

    scope.standardAdapter.subjectOption = "all";

    var query = scope.createStandardMongoQuery("ed", ["name", "title"]);

    var queryObject = JSON.parse(query);
    expect(queryObject).toEqual({ $or: [
        { subject: { $regex: '\\bed', $options: 'i' } },
        { name: { $regex: '\\bed', $options: 'i' } },
        { title: { $regex: '\\bed', $options: 'i' } }
      ] }
    );

    scope.standardAdapter.subjectOption = { name: "ELA" };

    var elaQuery = JSON.parse(scope.createStandardMongoQuery("ed", ["dotNotation"]));

    expect(elaQuery).toEqual(
      {
        $or: [
          { dotNotation: { $regex: '\\bed', $options: 'i' } }
        ],
        subject: 'ELA'
      }
    );

    scope.standardAdapter.categoryOption = { name: "Maths"};


    expect(JSON.parse(scope.createStandardMongoQuery("ed", ["dotNotation"]))).toEqual(

      { $or: [
        { dotNotation: { $regex: '\\bed', $options: 'i' } }
      ],
        subject: 'ELA',
        category: 'Maths'
      }
    );

    scope.standardAdapter.subCategoryOption = "Arithmetic";

    expect(JSON.parse(scope.createStandardMongoQuery("ed", ["dotNotation"]))).toEqual(

      {
        $or: [
          { dotNotation: { $regex: '\\bed', $options: 'i' } }
        ],
        subject: 'ELA',
        category: 'Maths',
        subCategory: 'Arithmetic'
      }
    );

  });

  it('creates key skills summary', function () {

    expect(scope.getKeySkillsSummary(["a"])).toEqual("1 Key Skill selected");
    expect(scope.getKeySkillsSummary(["a", "b"])).toEqual("2 Key Skills selected");
    expect(scope.getKeySkillsSummary(["a", "b", "c"])).toEqual("3 Key Skills selected");
    expect(scope.getKeySkillsSummary([])).toEqual("No Key Skills selected");
    expect(scope.getKeySkillsSummary(null)).toEqual("No Key Skills selected");
    expect(scope.getKeySkillsSummary(undefined)).toEqual("No Key Skills selected");

  });

  xit('receives incremented item', function(){
    scope.itemData = {id: "itemId"};
    scope.itemData.increment = function(params,onSuccess,onError){
        console.log(JSON.stringify(params))
        var url = "/api/v1/items/:id/increment".replace(":id",params.id);
        http.post(url,{}).success(function(resource){
            onSuccess(resource);
        }).error(onError);
    };
    scope.increment();
    $httpBackend.flush();
    expect(location.path()).toEqual('/edit/itemId2');
  });

  describe('addDomain', function() {
    var newDomain = {name : 'new domain!'};

    describe('is not in itemData.domains', function() {
      beforeEach(function() {
        scope.itemData = {
          domains: [],
          standardDomains: []
        };
        scope.addDomain(newDomain);
        scope.$digest();
      });

      it('is added to itemData.domains', function() {
        expect(scope.itemData.domains.indexOf(newDomain.name)).not.toBe(-1);
      });
    });

    describe('it is in itemData.domains', function() {
      var domainsBefore;
      beforeEach(function() {
        scope.itemData = {
          domains: [newDomain.name],
          standardDomains: []
        };
        domainsBefore = _.clone(scope.itemData.domains);
        scope.addDomain(newDomain);
        scope.$digest();
      });

      it('does not change itemData.domains', function() {
        expect(scope.itemData.domains).toEqual(domainsBefore);
      });
    });

  });

  describe('removeDomain', function() {
    var removeMe = {name: 'remove this domain!'};
    beforeEach(function() {
      scope.itemData = {
        domains: [removeMe.name],
        standardDomains: []
      };
      scope.removeDomain(removeMe.name);
      scope.$digest();
    });

    it('removes domain from itemData.domains', function() {
      expect(scope.itemData.domains.indexOf(removeMe.name)).toEqual(-1);
    });
  });

});