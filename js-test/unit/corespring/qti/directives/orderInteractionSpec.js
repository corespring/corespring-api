describe('qtiDirectives.orderinteraction', function () {
    'use strict';

    var helper = new com.qti.helpers.QtiHelper();

    var basicNode = ['<orderInteraction responseIdentifier="question" maxChoices="${maxChoices}">',
        '<prompt>Prompt text</prompt>',
        '<simpleChoice identifier="a">A</simpleChoice>',
        '<simpleChoice identifier="b">B</simpleChoice>',
        '</orderInteraction>'
    ].join("\n");

    var getInteraction = function (node) {
        node = (node || basicNode.replace("${maxChoices}", 1));
        return helper.compileAndGetScope(rootScope, compile, node);
    };

    afterEach(function() {
        rootScope = null;
    });


    beforeEach(module('qti'));

    var rootScope, compile;

    beforeEach(inject(function ($compile, $rootScope, _$httpBackend_) {
        helper.prepareBackend(_$httpBackend_);
        rootScope = $rootScope.$new();

        rootScope.highlightCorrectResponse = function(){ return true};
        compile = $compile;
    }));

    describe('orderInteraction', function () {

        describe("compilation", function () {

            it('inits correctly', function () {
                var interaction = getInteraction()
                expect(interaction.scope).not.toBe(null);
                var element = interaction.element;
                expect(interaction.scope.prompt).toBe("Prompt text");
                expect(interaction.scope.orderedList.length).toBe(2);
            });

        });
    });
});
