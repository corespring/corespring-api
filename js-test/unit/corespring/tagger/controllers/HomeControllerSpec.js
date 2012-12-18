'use strict';

describe('HomeController', function () {

    // Mock dependencies
    var MockItemService = function() {};
    MockItemService.prototype.$save = jasmine.createSpy("Resource Save");
    MockItemService.createWorkflowObject = jasmine.createSpy("Create Workflow Object");

    var MockSearchService = function() {}
    MockSearchService.search = jasmine.createSpy("Search");

    beforeEach(function () {
        module(function ($provide) {
            $provide.value('ItemService', MockItemService);
            $provide.value('ServiceLookup', {});
            $provide.value('SupportingMaterial', {});
            $provide.value('SearchService', MockSearchService);
            $provide.value('Collection', {
                query: function(data, result) {
                    setTimeout(result, 0);
                    return ["collection1", "collection2"];
                },
                get: function() {

                }
            });

        }, 'corespring-utils');
    });

    var scope, ctrl, $httpBackend;

    var prepareBackend = function ($backend) {

        var urls = [
            {method:'PUT', url:/.*/, response:{ ok:true }},
            {method:'POST', url:/.*/, data: {}, response:{ ok:true }}
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
        scope.search = function() {

        }

        try {
            ctrl = $controller(HomeController, {$scope:scope});
        } catch (e) {
            throw("Error with the controller: " + e);
        }
    }));

    describe("inits", function(){

        it("is inited correctly", function(){

            expect(ctrl).not.toBeNull();
        });

        it("creates a sorted grade level string", function(){

            expect(
                scope.createGradeLevelString(["01","KG","Other"]) )
                .toEqual( "KG,01,Other")
        });

        it("creates a primary subject label", function(){

            var subj = {
                category: "Category",
                subject: "Subject"
            };

            expect(
                scope.getPrimarySubjectLabel( subj )
            ).toBe( subj.category + ": " + subj.subject);

            var subjNoCategory = {
                subject: "Subject"
            };

            expect(
                scope.getPrimarySubjectLabel(subjNoCategory)
            ).toBe(subjNoCategory.subject);

            var subjNoSubject = {
                category: "Category"
            };

            expect(
                scope.getPrimarySubjectLabel(subjNoSubject)
            ).toBe(subjNoSubject.category);
        });

        it("builds a standard label", function(){

            expect(scope.buildStandardLabel([])).toBe("");

            var s = [
                { dotNotation: "dotNotation"}
            ];

            expect(scope.buildStandardLabel(s)).toBe(s[0].dotNotation);

            s.push( { dotNotation: "dotNotation" } );

            expect(scope.buildStandardLabel(s)).toBe(s[0].dotNotation + " plus 1 more");
        });

        it("builds a standards tooltip", function(){

            expect(scope.buildStandardTooltip([])).toBe("");

            var s = [
                { standard: "s", dotNotation: "dn"}
            ];

            expect(scope.buildStandardTooltip(s)).toBe("s");

            s.push({ standard: "a b c d e f g", dotNotation: "dn2"});

            expect(scope.buildStandardTooltip(s)).toBe("dn: s, dn2: a b c d e f...");
        });

        it("Search should invoke search service", function() {
            MockSearchService.search = jasmine.createSpy("Search").andCallFake(function(params, handler) {
                handler(["item"]);
            });
            scope.search();
            expect(MockSearchService.search).toHaveBeenCalled();
            expect(scope.items).toEqual(["item"]);
        });
    });

});
