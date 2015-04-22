describe('item-action-buttons', function(){

  beforeEach(module('tagger.directives'));

  var rootScope, compile;

  var div = ['<item-action-button',
              '  org-drafts="orgDrafts"',
              '  user-name="userName"',
              '  org="org"',
              '  item="item"',
              '  publish="publish"',
              '  clone-item="cloneItem"',
              '  delete-item="deleteItem"',
              '  delete-draft="$parent.deleteDraft"',
              '>',
              '</item-action-button>'
  ].join('');

  beforeEach(inject(function($rootScope, $compile){
    rootScope = $rootScope;
    compile = $compile;
  }));
  
  function mkScope(){
    var out = rootScope.$new();
    out.orgDrafts = [];
    out.userName = 'Ed';
    out.org = { id: 'o1'};
    out.item = { id: 'i1'};
    out.publish = jasmine.createSpy('publish');
    out.cloneItem = jasmine.createSpy('cloneItem');
    out.deleteItem = jasmine.createSpy('deleteItem');
    out.deleteDraft = [];
    return out; 
  } 

  function mkDirective(s){
    var out = compile(angular.element('<item-action-button org-drafts="orgDrafts">'))(s);
    out.scope().$digest(); 
    return out;
  }

  describe('init', function(){

    var scope, directiveScope;

    beforeEach(function(){
      scope = mkScope(); 
      directiveScope = mkDirective(scope);
    });

    it('compiles', function(){
      expect(scope).not.toBe(null);
      expect(directiveScope).not.toBe(null);
    }); 

  });
  
  describe('ownsDraft', function(){

    var scope, directiveScope;

    beforeEach(function(){
      scope = mkScope();
      scope.orgDrafts = [{id: 'd1', orgId: 'o1', user: 'Ed', itemId: 'i1'}];
      directiveScope = mkDirective(scope);
    });

    it('sets status to ownsDraft', function(){
      expect(scope.draftStatus).toEqual('ownsDraft');
    });
  });

});