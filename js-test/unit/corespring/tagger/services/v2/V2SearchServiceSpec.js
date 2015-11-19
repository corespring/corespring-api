describe('V2SearchService', function () {

  var service, httpBackend;

  beforeEach(module('tagger.services'));

  beforeEach(inject(function ($httpBackend, V2SearchService) {
    httpBackend = $httpBackend;
    service = V2SearchService;
  }));


  describe('init', function () {
    it('initializes', function () {
      expect(service).toBe(service);
    });
  });

  describe('search', function(){
    var capturedUrl = null;

    beforeEach(function(){
      capturedUrl = null;
      httpBackend.when('GET', /(.*)/).respond(function(method, url){
        capturedUrl = url;
        return {total: 0, hits: []};
      });
    });

    function getSearch(url){
      var parts = url.split('?').pop().split('&');
      var result = {};
      for(var i=0; i<parts.length; i++){
        var param = parts[i].split('=');
        var name = param.shift();
        var value = decodeURIComponent(param.shift());
        result[name] = value;
      }
      return result;
    }

    function expectedQuery(searchText){
      return JSON.stringify({
        text:searchText,
        contributors:[],
        collections:[],
        gradeLevels:[],
        itemTypes:[],
        widgets:[],
        workflows:[]
      });
    }

    it('can search for ampersand', function(){
      service.search({searchText:'&'});
      httpBackend.flush();
      expect(getSearch(capturedUrl).query).toEqual(expectedQuery('&'));
    });

    it('can search for question mark', function(){
      service.search({searchText:'?'});
      httpBackend.flush();
      expect(getSearch(capturedUrl).query).toEqual(expectedQuery('?'));
    });

    it('can search for single quote', function(){
      service.search({searchText:'\''});
      httpBackend.flush();
      expect(getSearch(capturedUrl).query).toEqual(expectedQuery('\''));
    });

    it('can search for double quote', function(){
      service.search({searchText:'"'});
      httpBackend.flush();
      expect(getSearch(capturedUrl).query).toEqual(expectedQuery('"'));
    });

  });
});