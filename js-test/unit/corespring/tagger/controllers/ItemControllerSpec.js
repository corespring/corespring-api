'use strict';

describe('ItemController should', function () {

    beforeEach(function () {
        angular.module('tagger.services')
            .factory('AccessToken', [ function () {
            return { token:"1" };
        }]
        );
    });

    var scope, ctrl, $httpBackend;

    beforeEach(module('tagger.services'));

    var prepareBackend = function ($backend) {

        var urls = [
            {method:'PUT', url:/.*/, response:{ ok:true }},
            {method:'POST', url:/.*/, data:{}, response:{ ok:true }},
            {method: 'GET', url: "/api/v1/collections?access_token=1", response: {

            }},
            {method: 'GET', url: "/api/v1/items?access_token=1", response: {

            }},

            {method: 'GET', url: "/assets/web/standards_tree.json", response: {

            }}
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

        try {
            ctrl = $controller(ItemController, {$scope:scope});
        } catch (e) {
            throw("Error with the controller: " + e);
        }
    }));


    it('init correctly', inject(function () {
        expect(ctrl).not.toBeNull();
    }));

    it('calculates pValue as string', function(){
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

});
