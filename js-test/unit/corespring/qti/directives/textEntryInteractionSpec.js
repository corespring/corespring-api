describe('qtiDirectives.textentryinteraction', function () {
    'use strict';

    var helper = new com.qti.helpers.QtiHelper();


    var getInteraction = function () {
        var node = '<textentryinteraction responseIdentifier="rid" expectedLength="1"/>';
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
            var node = '<textentryinteraction responseIdentifier="rid" expectedLength="1"/>';
            var interaction = helper.compileAndGetScope(rootScope, compile, node);
            expect(interaction.scope.expectedLength).toBe('1');
        });

    });

    describe("behaviour", function () {
        it('interacts with controller', function () {
            var node = '<textentryinteraction responseIdentifier="rid" expectedLength="1"/>';
            var interaction = helper.compileAndGetScope(rootScope, compile, node);
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
            var interaction = helper.compileAndGetScope(rootScope, compile, node);
            var scope = interaction.scope;
            scope.textResponse = "";
            scope.$apply(function () {
                scope.showNoResponseFeedback = true;
            });

            expect(scope.noResponse).toBe(true);
        });

        it('updates the ui on response received', function () {
            var interaction = getInteraction();

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