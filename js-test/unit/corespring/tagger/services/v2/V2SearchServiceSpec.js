describe('V2SearchService', function() {

  var service, httpBackend;

  beforeEach(module('tagger.services'));

  beforeEach(inject(function($httpBackend, V2SearchService) {
    httpBackend = $httpBackend;
    service = V2SearchService;
  }));


  describe('init', function() {
    it('initializes', function() {
      expect(service).toBe(service);
    });
  });

  describe('search', function() {
    var capturedUrl = null;

    beforeEach(function() {
      capturedUrl = null;
      httpBackend.when('GET', /(.*)/).respond(function(method, url) {
        capturedUrl = url;
        return {
          total: 0,
          hits: []
        };
      });
    });

    function getSearch(url) {
      var parts = url.split('?').pop().split('&');
      var result = {};
      for (var i = 0; i < parts.length; i++) {
        var param = parts[i].split('=');
        var name = param.shift();
        var value = decodeURIComponent(param.shift());
        result[name] = value;
      }
      return result;
    }

    function expectedQuery(searchText, offset) {
      var query = {
        text: searchText,
        contributors: [],
        collections: [],
        gradeLevels: [],
        itemTypes: [],
        widgets: [],
        workflows: []
      };
      if (!angular.isUndefined(offset)) {
        query.offset = offset;
      }
      return JSON.stringify(query);
    }

    function expectedQueryWithOffset(searchText) {
      return expectedQuery(searchText, 0);
    }

    function getQuery() {
      httpBackend.flush();
      return getSearch(capturedUrl).query
    }

    describe('can search for', function() {

      it('ampersand', function() {
        service.search({
          searchText: '&'
        });
        expect(getQuery()).toEqual(expectedQuery('&'));
      });

      it('question mark', function() {
        service.search({
          searchText: '?'
        });
        expect(getQuery()).toEqual(expectedQuery('?'));
      });

      it('single quote', function() {
        service.search({
          searchText: '\''
        });
        expect(getQuery()).toEqual(expectedQuery('\''));
      });

      it('double quote', function() {
        service.search({
          searchText: '"'
        });
        expect(getQuery()).toEqual(expectedQuery('"'));
      });
    });

    describe('can loadMore of', function() {

      it('ampersand', function() {
        service.search({
          searchText: '&'
        });
        service.loadMore();
        expect(getQuery()).toEqual(expectedQueryWithOffset('&'));
      });

      it('question mark', function() {
        service.search({
          searchText: '?'
        });
        service.loadMore();
        expect(getQuery()).toEqual(expectedQueryWithOffset('?'));
      });

      it('single quote', function() {
        service.search({
          searchText: '\''
        });
        service.loadMore();
        expect(getQuery()).toEqual(expectedQueryWithOffset('\''));
      });

      it('double quote', function() {
        service.search({
          searchText: '"'
        });
        service.loadMore();
        expect(getQuery()).toEqual(expectedQueryWithOffset('"'));
      });
    });
  });
});