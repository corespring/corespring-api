describe('Modals', function () {
  'use strict';

  var service, rootScope;

  beforeEach(module('tagger.services'));

  beforeEach(inject(function ($rootScope, Modals) {
    rootScope = $rootScope;
    rootScope.modals = {
      clone: {}
    };
    service = Modals;
  }));

  describe('clone', function () {

    var item = {id: 'id'};

    var collections = [{name: 'Default'}];

    var done = jasmine.createSpy('done');

    it('inits', function(){
      expect(service).not.toBeUndefined();
    });

    beforeEach(function(){
      service.clone(item, collections, done);
    });

    it('sets show to true', function(){
      expect(rootScope.modals.clone.show).toBe(true);
    });
    
    it('sets item', function(){
      expect(rootScope.modals.clone.item).toBe(item);
    });
    
    it('sets collections', function(){
      expect(rootScope.modals.clone.collections).toBe(collections);
    });
    
    it('sets collection', function(){
      expect(rootScope.modals.clone.collection).toEqual({name: 'Default'});
    });
    
    it('sets agreed to false', function(){
      expect(rootScope.modals.clone.agreed).toBe(false);
    });

    it('calls done', function(){
      rootScope.modals.clone.done(false, false);
      expect(done).toHaveBeenCalledWith(false, false, collections[0]);
    });
  });

});