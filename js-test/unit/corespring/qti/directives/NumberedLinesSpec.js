describe('Testing Numbered lines directive', function() {
  var $scope, ctrl, compile;

  beforeEach(module('qti.directives'));

  beforeEach(inject(function($rootScope, $controller, $compile) {
    $scope = $rootScope.$new();
    compile = $compile;
  }));

  it('should say hallo to the World', function() {
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
  });
});