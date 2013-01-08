describe('lti-chooser.SearchController', function () {
  'use strict';

  var ctrl, scope, rootScope, $httpBackend;

  beforeEach(module('corespring-utils'));
  beforeEach(module('tagger.services'));
  beforeEach(module('corespring-services'));

  var prepareBackend = function ($backend) {

        var urls = [
            {method:'GET', url:/\/api\/v1\/collections.*/, response:{ ok:true }},
            {method:'POST', url:/.*/, data: {}, response:{ ok:true }}
        ];

        for (var i = 0; i < urls.length; i++) {
            var definition = urls[i];
            $backend.when(definition.method, definition.url).respond(200, definition.response);
        }
    };


  beforeEach(inject(function (_$httpBackend_, $rootScope, $controller) {
    $httpBackend = _$httpBackend_;
    rootScope = $rootScope;
    prepareBackend($httpBackend);
    scope = $rootScope.$new();

    try {
      ctrl = $controller(SearchController, {$scope: scope});
    } catch (e) {
      throw("Error with the controller: " + e);
    }
  }));


  describe('SearchController', function () {

    it('update isSearching', function () {
       rootScope.$broadcast('onNetworkLoading');
       expect(rootScope.isSearching).toBe(true);
       rootScope.$broadcast('onSearchCountComplete');
       expect(rootScope.isSearching).toBe(false);
    });

    it('should return item count', function(){
      expect(scope.getItemCountLabel(0)).toBe("0 results");
      expect(scope.getItemCountLabel(1)).toBe("1 result");
      expect(scope.getItemCountLabel(2)).toBe("2 results");
    })
 });
});