describe('qtiDirectives.assessmentItem', function () {
    'use strict';

    var helper = new com.qti.helpers.QtiHelper();

    var basicNode = [
        '<assessmentitem>',
            '<itembody>',
        '</itembody>',
        '</assessmentitem>'
    ].join("\n");


    var getInteraction = function (node) {
        var element = compile(node)(rootScope);
        return { element: element, scope: rootScope};
    };

    var getItemSession = function(){

        return {
            id: "id",
            itemId: "itemId",
            settings: {
                allowEmptyResponses: false
            }
        }
    };

    afterEach(function () {
        rootScope = null;
    });

    beforeEach(module('qti'));

    var rootScope, compile, interaction, itemSession, controller;

    beforeEach(inject(function ($compile, $rootScope, _$httpBackend_) {
        helper.prepareBackend(_$httpBackend_);
        rootScope = $rootScope.$new();
        compile = $compile;

        interaction = getInteraction(basicNode);
        controller = interaction.element.data('$assessmentitemController');
        itemSession = getItemSession();
    }));

    describe('assessment item', function() {

        it('should compile', function(){
            expect(interaction.scope).not.toBeNull();
            expect(controller).not.toBeNull();
            expect(interaction.element).not.toBeNull();
        });

        it('should init from an itemSession', function(){
            interaction.scope.itemSession = itemSession;
            expect(interaction.scope.itemSession).not.toBeNull();
        });

        it('should show responses is incorrect', function(){

            interaction.scope.itemSession = itemSession;

            controller.setResponse("questionOne", "apple");

            itemSession.sessionData = {};
            itemSession.sessionData.correctResponses = {
                questionOne: "banana"
            };
            //Need to get the server response set up for this
            //expect(interaction.scope.showResponsesIncorrect).toBe(true);

        });


    });
});
