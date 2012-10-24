'use strict';

describe('CreateController should', function () {


    // Mock dependencies
    var MockItemService = function() {};
    MockItemService.prototype.$save = jasmine.createSpy("Resource Save");

    beforeEach(function () {
        module(function($provide) {
            $provide.value('AccessToken', "1");
            $provide.value('ItemService', MockItemService);
            $provide.value('NewItemTemplates', { templ1:{label:"template 1", xmlData: "<xml>1</xml>"}, templ2: {label:"template 2", xmlData: "<xml>2</xml>"} });
            $provide.value('Collection', {
                            query: function(data, result) {
                                setTimeout(result, 0);
                                return ["collection1", "collection2"];
                            }
            });
        });
    });

    var scope, ctrl, $httpBackend;

    var prepareBackend = function ($backend) {

        var urls = [
            {method:'PUT', url:/.*/, response:{ ok:true }},
            {method:'POST', url:/.*/, data:{}, response:{ ok:true }},
            {method:'GET', url:"/api/v1/collections?access_token=1", response:{}},
            {method:'GET', url:"/api/v1/items?access_token=1", response:{}},
            {method:'GET', url:"/assets/web/standards_tree.json", response:{}}
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
            ctrl = $controller(CreateCtrl, {$scope:scope});
        } catch (e) {
            throw("Error with the controller: " + e);
        }
    }));


    it('init correctly', inject(function () {
        expect(ctrl).not.toBeNull();
        expect(scope.templates.length).toBe(2)
        expect(scope.collections.length).toBe(2)
        waitsFor(function() {
           return !!scope.selectedCollection;
        }, "Default collection didn't get selected", 100);
        runs(function() {
            expect(scope.selectedCollection).toBe('collection1')
        });
    }));

    it('need a selected template and collection for saving item ', inject(function () {
        scope.selectedCollection = undefined;
        scope.selectedTemplate = undefined;
        expect(scope.createItem()).toBeFalsy();

        scope.selectedCollection = undefined;
        scope.selectedTemplate = {};
        expect(scope.createItem()).toBeFalsy();

        scope.selectedCollection = {};
        scope.selectedTemplate = undefined;
        expect(scope.createItem()).toBeFalsy();
    }));

    it('creates new item ', inject(function () {
        scope.selectedTemplate = scope.templates[0];
        scope.selectedCollection = scope.collections[0];
        var item = scope.createItem();
        expect(item).not.toBeNull();
        expect(item.$save).toHaveBeenCalled();
        expect(item.data.name).toBe('qtiItem');
    }));

});
