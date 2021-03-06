describe('qtiDirectives.numberedLines', function () {

    'use strict';

    var $scope, compile;

    beforeEach(module('qti.directives'));

    beforeEach(inject(function ($rootScope, $controller, $compile) {
        $scope = $rootScope.$new();
        compile = $compile;
    }));

    describe('compilation', function () {
        it('numberedLines div should transform into ordered list and each line into an li', function () {
            var elm = compile(
                [
                    '<div class="numberedLines">',
                    '<line>Line 1</line>',
                    '<line>Line 2</line>',
                    '</div>'
                ].join(''))($scope);

            expect(elm.html()).toBe([
                '<ol ng-transclude="">',
                '<line class="ng-scope">',
                '<li ng-transclude="">',
                '<span class="ng-scope">Line 1</span>',
                '</li>',
                '</line>',
                '<line class="ng-scope">',
                '<li ng-transclude="">',
                '<span class="ng-scope">Line 2</span>',
                '</li>',
                '</line>',
                '</ol>'
            ].join(''));
        })
    })
});