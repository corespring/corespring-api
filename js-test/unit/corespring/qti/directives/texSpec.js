describe('qtiDirectives.tex', function () {

    'use strict';

    var $scope, compile;

    beforeEach(module('qti.directives'));

    beforeEach(inject(function ($rootScope, $controller, $compile) {
        $scope = $rootScope.$new();
        compile = $compile;
    }));

    describe('compilation', function () {
        it('default compilation', function () {
            var elm = compile('<tex>sample</tex>')($scope);
            expect(elm.html()).toBe('\\(sample\\)');
        });
        it('compilation with explicit inline true', function () {
            var elm = compile('<tex inline="true">sample</tex>')($scope);
            expect(elm.html()).toBe('\\(sample\\)');
        });
        it('compilation with explicit inline false', function () {
            var elm = compile('<tex inline="false">sample</tex>')($scope);
            expect(elm.html()).toBe('$$sample$$');
        });
    })
});