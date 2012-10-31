describe('qtiServices.QtiUtils', function () {

    'use strict';

    beforeEach(module('qti'));

    var service;
    beforeEach(inject(function ($injector) {

        try {
            service = $injector.get('QtiUtils');
        } catch (e) {
            throw("Error with the service: " + e);
        }
    }));


    describe('compare', function () {

        it("compares strings", function () {
            expect(function () {
                service.compare(undefined, undefined)
            }).toThrow(service.ERROR.undefinedElement);
            expect(service.compare(undefined, "apple")).toBe(false);
            expect(service.compare("apple", undefined)).toBe(false);
            expect(service.compare("apple", "apple")).toBe(true);
            expect(service.compare("apple", "banana")).toBe(false);
        });

        it("checks item is in array", function () {
            expect(service.compare("apple", ["apple"])).toBe(true);
            expect(service.compare("apple", ["banana"])).toBe(false);

        });

    });

    describe("compare response sets", function () {

        it("compares user responses against full response set", function () {
            var user = [
                    { "id":"mexicanPresident", "value":"cameron" },
                    { "id":"rainbowColors", "value":"white" },
                    { "id":"winterDiscontent", "value":"asdf" },
                    { "id":"cutePugs", "value":[ "pug2", "pug1", "pug3" ] }
                ];

            var fullset = [
                { "id":"mexicanPresident", "value":"calderon" },
                { "id":"rainbowColors", "value":[ "blue", "violet", "red" ] },
                { "id":"winterDiscontent", "value":[ "York", "york" ] },
                { "id":"cutePugs", "value":[ "pug1", "pug2", "pug3" ] }
            ]

        });


    });

    describe('getResponseById', function () {

        it("gets the response", function () {
            expect(service.getResponseById('one', [
                {id:'one', value:'hello'}
            ]).value).toBe('hello')
        });

        it("returns null if id or array is null", function () {
            expect(service.getResponseById('x', null)).toBe(null);
            expect(service.getResponseById(null, [])).toBe(null);
            expect(service.getResponseById('x', [])).toBe(null);
        });
    });

    describe('getResponseValue', function () {

        it("gets the value", function () {
            expect(
                service.getResponseValue("one", [
                    {id:"one", value:"hello" }
                ])).toBe("hello")

            expect(
                service.getResponseValue("one", [], "default")
            ).toBe("default");

            expect(
                service.getResponseValue("one", [])
            ).toBe("");
        });

    });

});
