describe('item-status', function () {
  'use strict';

  var myDiv;
  var scope;
  
  beforeEach(module('tagger'));

  beforeEach(inject(function ($compile, $rootScope) {
    myDiv = $('<item-status></item-status>');
    scope = $rootScope.$new();
    $compile(myDiv)(scope);
  }));

  describe("initialization", function () {

    it('should initialise', function(){
      expect(myDiv.html()).toEqual('?');
    });
  });


});

