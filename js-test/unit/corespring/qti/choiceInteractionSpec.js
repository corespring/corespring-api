describe('qtiDirectives.choiceinteraction', function () {
    'use strict';

    var prepareBackend = function ($backend) {

        var urls = [
            {
                method:'GET',
                url:'/api/v1/items/itemId/sessions/itemSessionId?access_token=34dj45a769j4e1c0h4wb',
                response:{"id":"itemSessionId", "itemId":"itemId", "start":1349970769197, "responses":[]}
            }
        ];

        for (var i = 0; i < urls.length; i++) {
            var definition = urls[i];
            $backend.when(definition.method, definition.url).respond(200, definition.response);
        }
    };

    var helper = new com.qti.helpers.QtiHelper();

    var wrap = function (content) {
        return helper.wrap(content);
    };


    var basicNode = ['<choiceInteraction responseIdentifier="question" maxChoices="${maxChoices}">',
        '<simpleChoice identifier="a">A</simpleChoice>',
        '<simpleChoice identifier="b">B</simpleChoice>',
        '</choiceInteraction>'
    ].join("\n");

    var getInteraction = function (node) {

        node = (node || basicNode.replace("${maxChoices}", 1));

        return compileAndGetScope(rootScope, compile, node);
    };

    var compileAndGetScope = function ($rootScope, $compile, node) {
        var element = $compile(wrap(node))($rootScope);
        console.log("element: " + element.html());
        return { element:element.children(), scope:$rootScope.$$childHead};
    };


    beforeEach(module('qti'));

    var rootScope, compile;

    beforeEach(inject(function ($compile, $rootScope, _$httpBackend_) {
        prepareBackend(_$httpBackend_);
        rootScope = $rootScope.$new();
        compile = $compile;
    }));

    describe("compilation", function () {
        it('inits checkboxes correctly', function () {
            var interaction = getInteraction(basicNode.replace("${maxChoices}", "0"));
            expect(interaction.scope).not.toBe(null);
            var element = interaction.element;
            expect(element.find('simplechoice').length).toBe(2);
            expect(element.find('input').attr('type')).toBe('checkbox');
        });

        it('inits radios correctly', function () {
            var interaction = getInteraction();
            expect(interaction.scope).not.toBe(null);
            var element = interaction.element;
            expect(element.find('simplechoice').length).toBe(2);
            expect(element.find('input').attr('type')).toBe('radio');
        });
    });

    describe("behaviour", function () {
        it('dummy', function () {
            expect(true).toBe(true);
        });
    });
});
