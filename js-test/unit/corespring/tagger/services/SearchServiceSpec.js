describe('SearchService', function () {

  var scope, service;
  var mongoQuery = new com.corespring.mongo.MongoQuery();
  var onResult = function(data){ console.log(data)};
  var onFault = function(data){ console.log(data)};

  beforeEach(module('tagger.services'));

  var captured = {

  };

  var MockItemService = function () {

    captured.query = null;
    captured.countQuery = null;

    this.queryCalledCount = 0;

    this.reset = function(){
      this.queryCalledCount = 0;
    };

    this.query = function(query, success, error){
      this.queryCalledCount += 1;
      captured.query = query;
      console.log(query);
      success([{name: "1"}]);
    };

    this.count = function(query, success){
      captured.countQuery = query;
      success({"count": 500});
    }
  };

  var mockItemService;

  beforeEach(function () {
    module(function ($provide) {

      mockItemService = new MockItemService();
      $provide.value('ItemService', mockItemService);
    });
  });

  beforeEach(inject(function ($rootScope, SearchService) {
    scope = $rootScope.$new();

    try {
      service = SearchService;
    } catch (e) {
      throw("Error with the service: " + e);
    }
  }));


  describe("init", function () {

    it("initializes", function () {
      expect(service).toBe(service);
    });

    it("searches with nothing specified", function(){

      var params = {};

      service.search(params, onResult, onFault);
      var resultFieldsAsString = JSON.stringify(mongoQuery.buildFilter(service.resultFields));
      expect(captured.query.f).toBe(resultFieldsAsString);
      expect(captured.query.q).toBe("{}");
      expect(captured.query.l).toBe(50);
      service.loadMore(onResult);
      expect(captured.query.f).toBe(resultFieldsAsString);
      expect(captured.query.q).toBe("{}");
      expect(captured.query.l).toBe(50);
      expect(captured.query.sk).toBe(50);
      expect(mockItemService.queryCalledCount).toBe(2);

    });

    it("load more - skips by the amount specified in the params", function(){
      var params = {};
      service.search(params, onResult, onFault);
      expect(service.resultCount).toBe(500);
      service.loadMore(onResult);
      expect(captured.query.sk).toBe(50);
      service.loadMore(onResult);
      expect(captured.query.sk).toBe(100);
      service.loadMore(onResult);
      expect(captured.query.sk).toBe(150);
    });

    it("calls with searchText", function(){

      var params = { searchText : "a"};

      var textQuery = mongoQuery.fuzzyTextQuery(params.searchText, service.searchFields);

      service.search(params, onResult, onFault);
      var resultFieldsAsString = JSON.stringify(mongoQuery.buildFilter(service.resultFields));
      expect(captured.query.f).toBe(resultFieldsAsString);
      expect(captured.query.q).toBe(JSON.stringify(textQuery));
      expect(captured.query.l).toBe(50);
      service.loadMore(onResult);
      expect(captured.query.f).toBe(resultFieldsAsString);
      expect(captured.query.q).toBe(JSON.stringify(textQuery));
      expect(captured.query.l).toBe(50);
      expect(captured.query.sk).toBe(50);

      expect(mockItemService.queryCalledCount).toBe(2);

    });

    it("search for properties", function(){
      var params = { gradeLevel: [{ key: "01"}]};
      service.search(params, onResult, onFault);
      expect(captured.query.q).toBe('{"gradeLevel":{"$in":["01"]}}');
      var params2 = { gradeLevel: { key: "01"}};
      service.search(params2, onResult, onFault);
      expect(captured.query.q).toBe('{"gradeLevel":"01"}');
    });

    it("passes sort", function(){

      var params = { sort: {gradeLevel: 1}};
      service.search(params, onResult, onFault);
      expect(captured.query.sort).toBe('{"gradeLevel":1}');
      service.loadMore(onResult);
      expect(captured.query.sort).toBe('{"gradeLevel":1}');
      expect(mockItemService.queryCalledCount).toBe(2);

    });

  });
});