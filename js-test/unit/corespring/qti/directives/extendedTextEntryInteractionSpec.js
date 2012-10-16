describe('qtiDirectives.extendedtextentryinteraction', function () {
    'use strict';

    var helper = new com.qti.helpers.QtiHelper();

    beforeEach(module('qti'));

    var rootScope, compile;

    beforeEach(inject(function ($compile, $rootScope, _$httpBackend_) {
        helper.prepareBackend(_$httpBackend_);
        rootScope = $rootScope.$new();
        compile = $compile;
    }));

    describe("compilation", function () {

        it('inits correctly', function () {
            var node = '<extendedtextinteraction responseIdentifier="rid" expectedLines="5"/>';
            var interaction = helper.compileAndGetScope(rootScope, compile, node);
            expect(interaction.scope.rows).toBe('5');
        });

    });

    describe("behaviour", function () {
        it('interacts with controller', function () {
            var node = '<extendedtextinteraction responseIdentifier="rid" expectedLength="1"/>';
            var interaction = helper.compileAndGetScope(rootScope, compile, node);
            var scope = interaction.scope;
            var response = "here's a response";

            scope.$apply(function () {
                scope.extResponse = response;
            });

            expect(scope.extResponse).toBe(response);
            expect(scope.controller.findItemByKey("rid").value).toBe(response);
        });

        it('shows no response feedback when empty', function () {
            var node = '<extendedtextinteraction responseIdentifier="rid" expectedLength="1"/>';
            var interaction = helper.compileAndGetScope(rootScope, compile, node);
            var scope = interaction.scope;
            scope.extResponse = "";
            scope.$apply(function () {
                scope.showNoResponseFeedback = true;
            });

            expect(scope.noResponse).toBe(true);
        });

        it('doesnt show no response feedback when nonempty', function () {
            var node = '<extendedtextinteraction responseIdentifier="rid" expectedLength="1"/>';
            var interaction = helper.compileAndGetScope(rootScope, compile, node);
            var scope = interaction.scope;
            scope.extResponse = "Some text";
            scope.$apply(function () {
                scope.showNoResponseFeedback = true;
            });

            expect(scope.noResponse).toBe(false);
        });
    });
});