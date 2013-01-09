describe('qtiDirectives.feedbackinline', function () {
    'use strict';

    var helper = new com.qti.helpers.QtiHelper();

    var getFeedback = function (node) {
        return helper.compileAndGetScope(rootScope, compile, node);
    };

    beforeEach(module('qti'));

    var rootScope, compile;

    beforeEach(inject(function ($compile, $rootScope, _$httpBackend_) {
        helper.prepareBackend(_$httpBackend_);
        rootScope = $rootScope.$new();
        compile = $compile;
    }));

    describe("compilation", function () {
        it('inits correctly', function () {
            var node = '<feedbackinline csFeedbackId="a"></feedbackinline>';
            var interaction = getFeedback(node);
            expect(interaction.scope).not.toBeNull();
            expect(interaction.scope).not.toBeUndefined();
        });
    });


    describe("behaviour", function(){

        it('responds to correctResponses', function(){

            var node = '<feedbackinline csFeedbackId="a"></feedbackinline>';
            var interaction = getFeedback(node);

            rootScope.itemSession= {};
            rootScope.itemSession.sessionData = {};

            helper.setSessionSettings(rootScope, {showFeedback: true, highlightUserResponse: true, highlightCorrectResponse: true });
            helper.finishSession(rootScope, true);

            rootScope.$apply(function () {
                rootScope.itemSession.sessionData.feedbackContents = { a: "correct!"};
                rootScope.itemSession.sessionData.correctResponses = { rid:"correct" }
            });

            expect(interaction.scope.feedback).toBe("correct!");

            rootScope.$apply(function () {
                rootScope.itemSession.sessionData.feedbackContents = { b: "correct!"};
                rootScope.itemSession.sessionData.correctResponses = { rid:"rect" }
            });

            expect(interaction.scope.feedback).toBe("");
        });

        it('does not show feedback when feedback is disabled', function () {

            var node = '<feedbackinline csFeedbackId="a"></feedbackinline>';
            var interaction = getFeedback(node);

            rootScope.itemSession= {};
            rootScope.itemSession.sessionData = {};

            helper.setFeedbackEnabled(rootScope,rootScope.itemSession, false);

            rootScope.$apply(function(){
                rootScope.itemSession.sessionData.feedbackContents = { a: "correct!"};
                rootScope.itemSession.sessionData.correctResponses = { rid:"correct" }
            });

            expect(interaction.scope.feedback).toBe("")

        });

        it('resets ui', function(){
            var node = '<feedbackinline csFeedbackId="a"></feedbackinline>';
            var interaction = getFeedback(node);

            rootScope.itemSession= {};
            rootScope.itemSession.sessionData = {};


            helper.setSessionSettings(rootScope, {showFeedback: true, highlightUserResponse: true, highlightCorrectResponse: true });
            helper.finishSession(rootScope, true);

            rootScope.$apply(function () {
                rootScope.itemSession.sessionData.feedbackContents = { a: "correct!"};
                rootScope.itemSession.sessionData.correctResponses = { rid:"correct" }
            });

            expect(interaction.scope.feedback).toBe("correct!");

            rootScope.$broadcast('resetUI');

            expect(interaction.scope.feedback).toBe("");

        });
    });
});
