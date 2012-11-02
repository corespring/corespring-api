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
        return { element:element, scope:rootScope};
    };

    var getItemSession = function (sessionId, itemId, settings) {

        sessionId = (sessionId || "sessionId");
        itemId = (itemId || "itemId");
        settings = (settings || { allowEmptyResponses:false});
        return {
            id:sessionId,
            itemId:itemId,
            settings:settings
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

    describe('assessment item', function () {

        it('should compile', function () {
            expect(interaction.scope).not.toBeNull();
            expect(controller).not.toBeNull();
            expect(interaction.element).not.toBeNull();
        });

        it('should init from an itemSession', function () {
            interaction.scope.itemSession = itemSession;
            expect(interaction.scope.itemSession).not.toBeNull();
        });


        var setUpBackendForSubmit = function (getResponse) {
            interaction.scope.$apply(function () {
                interaction.scope.itemSession = getItemSession(Math.random() + "", 'itemId');
                itemSession = interaction.scope.itemSession;
            });
            var def = TestPlayerRoutes.api.v1.ItemSessionApi.update(itemSession.itemId, itemSession.id);
            httpBackend.when(def.method, def.url).respond(200, getResponse());
        };

        var assertFormIncorrect = function (isCorrect, responses) {

            setUpBackendForSubmit(function () {
                var response = angular.copy(itemSession);
                response.responses = responses;
                return response;
            });

            controller.submitResponses();
            httpBackend.flush();
            expect(interaction.scope.formHasIncorrect).toBe(isCorrect);
        };

        it('form is incorrect if any item has a score of 0', function () {
            assertFormIncorrect(true, [
                { id:"questionOne", value:"apple", outcome:{ score:0 } }
            ]);
            assertFormIncorrect(false, [
                { id:"questionOne", value:"apple", outcome:{ score:1 } }
            ]);
            assertFormIncorrect(false, [
                { id:"questionOne", value:"apple" }
            ]);
        });

        it('sets finalSubmit to true and sets it to false if the user makes a change', function () {

            setUpBackendForSubmit(function () {
                var response = angular.copy(itemSession);
                response.responses = [];
                return response;
            });

            controller.submitResponses();
            httpBackend.flush();
            expect(interaction.scope.finalSubmit).toBe(true);
            controller.setResponse("questionOne", "hello");
            expect(interaction.scope.finalSubmit).toBe(false);
        });

        it('sets can submit correctly', function () {

            interaction.scope.$apply(function () {
                interaction.scope.itemSession = getItemSession(Math.random() + "", 'itemId', {
                    allowEmptyResponses:false
                });
                itemSession = interaction.scope.itemSession;
            });

            expect(interaction.scope.canSubmit).toBe(false);

            controller.setResponse("questionOne", "hello");

            expect(interaction.scope.canSubmit).toBe(true);

            controller.setResponse("questionOne", null);

            interaction.scope.$apply(function () {
                interaction.scope.itemSession =
                    getItemSession(
                        Math.random() + "",
                        'itemId',
                        { allowEmptyResponses:true }
                    );
                itemSession = interaction.scope.itemSession;

            });

            expect(interaction.scope.canSubmit).toBe(true);
        });

        it('checks has empty response', function () {
            var scope = interaction.scope;
            scope.responses = [];
            expect(scope.hasEmptyResponse()).toBe(false);
            scope.responses = [
                {id:"a", value:""}
            ];
            expect(scope.hasEmptyResponse()).toBe(true);
            scope.responses = [
                {id:"a", value:"a"}
            ];
            expect(scope.hasEmptyResponse()).toBe(false);
            scope.responses = [
                {id:"aa", value:"aa"},
                {id:"a", value:""}
            ];
            expect(scope.hasEmptyResponse()).toBe(true);
        });

        it('checks is empty item', function () {
            var scope = interaction.scope;
            expect(scope.isEmptyItem(null)).toBe(true);
            expect(scope.isEmptyItem(undefined)).toBe(true);
            expect(scope.isEmptyItem("")).toBe(true);
            expect(scope.isEmptyItem([])).toBe(true);
            expect(scope.isEmptyItem("hello")).toBe(false);
            expect(scope.isEmptyItem(["hello"])).toBe(false);
        });

        it('handles reset correctly', function () {
            rootScope.formSubmitted = true;
            rootScope.formHasIncorrect = true;
            rootScope.finalSubmit = true;
            rootScope.$broadcast('reset');
            expect(rootScope.formSubmitted).toBe(false);
            expect(rootScope.formHasIncorrect).toBe(false);
            expect(rootScope.finalSubmit).toBe(false);
        });

        it('shows no response feedback', function(){

            setUpBackendForSubmit(function () {
                var response = angular.copy(itemSession);
                response.responses = interaction.scope.responses;
                return response;
            });

            controller.setResponse("questionOne", "hello");
            expect(rootScope.showNoResponseFeedback).toBe(false);
            controller.setResponse("questionOne", null);
            expect(rootScope.showNoResponseFeedback).toBe(false);
            controller.submitResponses();
            expect(rootScope.showNoResponseFeedback).toBe(true);
            controller.setResponse("questionOne", "hello");
            controller.submitResponses();
            expect(rootScope.showNoResponseFeedback).toBe(false);

        });
    });
});
