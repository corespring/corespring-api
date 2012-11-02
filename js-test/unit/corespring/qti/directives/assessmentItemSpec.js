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

    var getItemSession = function(sessionId, itemId, settings){

        sessionId = (sessionId || "sessionId");
        itemId = (itemId || "itemId");
        settings = (settings || { allowEmptyResponses: false});
        return {
            id: sessionId,
            itemId: itemId,
            settings: settings
        }
    };

    afterEach(function () {
        rootScope = null;
    });

    beforeEach(module('qti'));

    var rootScope, compile, interaction, itemSession, controller, httpBackend;

    beforeEach(inject(function ($compile, $rootScope, _$httpBackend_) {
        httpBackend = _$httpBackend_;
        helper.prepareBackend(_$httpBackend_);
        rootScope = $rootScope.$new();
        compile = $compile;

        interaction = getInteraction(basicNode);
        controller = interaction.element.data('$assessmentitemController');
        itemSession = getItemSession();
    }));

    /**
     * the responses are added in the submt function,
     * but we don't have access to the submitted data
     * TODO: See if there's a way of accessing this data
     * See: https://groups.google.com/forum/?fromgroups=#!topic/angular/WzuBKosy8PM
     */

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

        var assertFormIncorrect = function( isCorrect, responses  ) {

            interaction.scope.$apply(function(){
                interaction.scope.itemSession = getItemSession( Math.random() + "", 'itemId');
                itemSession = interaction.scope.itemSession;
            });

            var def = TestPlayerRoutes.api.v1.ItemSessionApi.update(itemSession.itemId, itemSession.id);
            var response = angular.copy(itemSession);
            response.responses = responses;
            httpBackend.when(def.method, def.url).respond(200,response);
            controller.submitResponses();
            httpBackend.flush();
            expect(interaction.scope.formHasIncorrect).toBe(isCorrect);
        };

        it('form is incorrect if any item has a score of 0', function(){
            assertFormIncorrect(true, [{ id : "questionOne", value: "apple", outcome: { score: 0 } }]);
            assertFormIncorrect(false, [{ id : "questionOne", value: "apple", outcome: { score: 1 } }]);
            assertFormIncorrect(false, [{ id : "questionOne", value: "apple" }]);
        });


    });
});
