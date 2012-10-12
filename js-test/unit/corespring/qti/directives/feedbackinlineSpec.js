describe('qtiDirectives.feedbackinline', function () {
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


    var getFeedback = function (node) {
        return compileAndGetScope(rootScope, compile, node);
    };

    var compileAndGetScope = function ($rootScope, $compile, node) {
        var element = $compile(wrap(node))($rootScope);
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

        it('resets ui', function(){
            var node = '<feedbackinline csFeedbackId="a"></feedbackinline>';
            var interaction = getFeedback(node);

            rootScope.itemSession= {};
            rootScope.itemSession.sessionData = {};

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
