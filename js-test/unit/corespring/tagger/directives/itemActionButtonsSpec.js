describe('item-action-buttons', function(){

  beforeEach(module('tagger.directives'));

  var rootScope, compile;

  beforeEach(inject(function($rootScope, $compile){
    rootScope = $rootScope;
    compile = $compile;
  }));

  function mkScope(){
    var out = rootScope.$new();
    out.item = { id: 'i1' };
    out.publish = jasmine.createSpy('publish');
    out.cloneItem = jasmine.createSpy('cloneItem');
    out.deleteItem = jasmine.createSpy('deleteItem');
    return out;
  }

  function mkDirective(s){
    var out = compile(
      ['<item-action-button',
      '  item="item"',
      '  publish="publish"',
      '  clone-item="cloneItem"',
      '  delete-item="deleteItem"',
      '>',
      '</item-action-button>'
    ].join(''))(s);
    out.scope().$digest();
    return out;
  }

  describe('init', function(){

    var scope, directive;

    beforeEach(function(){
      scope = mkScope();
      directive = mkDirective(scope);
    });

    it('compiles', function(){
      expect(scope).not.toBe(null);
      expect(directive).not.toBe(null);
    });

  });

  describe('visibility', function(){

    var scope, directive;

    beforeEach(function(){
      scope = mkScope();
    });

    it('is visible when item can be edited', function(){
      directive = mkDirective(scope);
      expect($(directive).css('display')).toEqual('');
    });

    it('is not visible when item is readonly', function(){
      scope.item.readOnly = true;
      directive = mkDirective(scope);
      expect($(directive).css('display')).toEqual('none');
    });
  });

  describe('publish button', function(){
    var scope, directive;

    function findPublishButton(){
      return $(directive).find('li [ng-click="publish(item)"]').parent();
    }

    beforeEach(function(){
      scope = mkScope();
    });

    it('it shows publish button when item is not published', function(){
      scope.item.published = false;
      directive = mkDirective(scope);
      expect(findPublishButton().css('display')).toEqual('');
    });

    it('it hides publish button when item is published', function(){
      scope.item.published = true;
      directive = mkDirective(scope);
      expect(findPublishButton().css('display')).toEqual('none');
    });
  });

});

