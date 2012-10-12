describe('textentryinteraction', function () {
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


    var getInteraction = function () {
        var node = '<textentryinteraction responseIdentifier="rid" expectedLength="1"/>';
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


        it('inits correctly', function () {
            var node = '<textentryinteraction responseIdentifier="rid" expectedLength="1"/>';
            var interaction = compileAndGetScope(rootScope, compile, node);
            expect(interaction.scope.expectedLength).toBe('1');
        });

    });

    describe("behaviour", function () {
        it('interacts with controller', function () {
            var node = '<textentryinteraction responseIdentifier="rid" expectedLength="1"/>';
            var interaction = compileAndGetScope(rootScope, compile, node);
            var scope = interaction.scope;
            var response = "here's a response";

            scope.$apply(function () {
                scope.textResponse = response;
            });

            expect(scope.textResponse).toBe(response);
            expect(scope.controller.findItemByKey("rid").value).toBe(response);
        });

        it('shows/hides no response feedback', function () {
            var node = '<textentryinteraction responseIdentifier="rid" expectedLength="1"/>';
            var interaction = compileAndGetScope(rootScope, compile, node);
            var scope = interaction.scope;
            scope.textResponse = "";
            scope.$apply(function () {
                scope.showNoResponseFeedback = true;
            });

            expect(scope.noResponse).toBe(true);
        });

        it('updates the ui on response received', function () {
            var interaction = getInteraction();
            console.log("itemSession: " + rootScope.itemSession);

            var element = interaction.element;
            var scope = interaction.scope;

            scope.$apply(function () {
                scope.textResponse = "correct";
            });

            rootScope.$apply(function () {
                rootScope.itemSession.sessionData = {};
                rootScope.itemSession.sessionData.correctResponses = { rid:"correct" }
            });

            expect(element.attr('class').contains(scope.CSS.correct)).toBe(true);

            scope.$apply(function () {
                scope.textResponse = "incorrect";
            });

            rootScope.$apply(function () {
                rootScope.itemSession.sessionData = {};
                rootScope.itemSession.sessionData.correctResponses = { rid:"correct" }
            });

            expect(element.attr('class').contains(' ' + scope.CSS.correct)).toBe(false);
        });

        it('resets the ui', function () {
            var interaction = getInteraction();
            var element = interaction.element;
            var scope = interaction.scope;

            element
                .addClass(scope.CSS.correct)
                .addClass(scope.CSS.incorrect);

            rootScope.$broadcast('resetUI');

            expect(element.attr('class').contains(' ' + scope.CSS.correct)).toBe(false);
            expect(element.attr('class').contains(' ' + scope.CSS.incorrect)).toBe(false);


        });
    });
});