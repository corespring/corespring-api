'use strict';

describe('HomeController', function () {

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
    });

});
