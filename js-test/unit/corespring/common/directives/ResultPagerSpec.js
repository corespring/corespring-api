describe('corespring-directives', function () {
  'use strict';


  var basicNode = ['<span ',
    'result-pager load-item="loadItem"',
    'list-model="items"',
    'current-item="currentItem"',
    'item-count="count"',
    'load-more="loadMore"></span>'].join("\n");


  beforeEach(module('corespring-directives'));

  var rootScope, compile, element;

  beforeEach(inject(function ($compile, $rootScope) {
    rootScope = $rootScope.$new();
    compile = $compile;

    element = compile(basicNode)(rootScope);
  }));


  describe('ResultPager', function () {


    var addItems = function (count, startingIndex) {

      startingIndex = (startingIndex || 0);

      var arr = [];
      for (var i = 0; i < count; i++) {
        arr.push({id: (i + startingIndex)});
      }

      rootScope.$apply(function () {
        rootScope.items = arr;
        rootScope.count = arr.length;
        rootScope.currentItem = arr[0];
      });
    };


    it('should compile', function () {
      expect(element).not.toBe(null);
      expect(rootScope.pagerText).toBe(null);
    });

    it('should show pager text', function () {

      addItems(1);

      expect(rootScope.pagerText).toBe("1 of 1");

      addItems(3);

      expect(rootScope.pagerText).toBe("1 of 3");

      rootScope.$apply(function () {
        rootScope.currentItem = null;
      });

      expect(rootScope.pagerText).toBe(null);

      rootScope.$apply(function () {
        rootScope.currentItem = {id: 2};
      });

      expect(rootScope.pagerText).toBe("3 of 3");

    });

    it('should add previous to scope', function () {

      addItems(3);

      rootScope.$apply(function () {
        rootScope.currentItem = rootScope.items[2];
      });

      var loadItemId = -1;
      rootScope.loadItem = function (id) {
        loadItemId = id;
      };

      rootScope.previous();
      expect(loadItemId).toBe(1);

      rootScope.$apply(function () {
        rootScope.currentItem = rootScope.items[1];
      });

      rootScope.previous();
      expect(loadItemId).toBe(0);
    });


    it('should add next to scope', function () {

      addItems(3);

      var loadItemId = -1;
      rootScope.loadItem = function (id) {
        loadItemId = id;
      };

      rootScope.next();
      expect(loadItemId).toBe(1);

      rootScope.$apply(function () {
        rootScope.currentItem = rootScope.items[1];
      });

      rootScope.next();
      expect(loadItemId).toBe(2);

    });

    it('if there are no more items it shoudl call load more', function () {

      addItems(3);

      rootScope.$apply(function () {
        rootScope.currentItem = rootScope.items[2];
        //need to simulate a higher count that whats in the array
        rootScope.count = 6;
      });

      var loadMoreIndex = -1;
      rootScope.loadMore = function (index, callback) {
        addItems(6);
        console.log("rootScope.items: " + rootScope.items);
        //rootScope.count = rootScope.items.length;
        loadMoreIndex = index;
        callback();
      };

      var loadItemIndex = -1;
      rootScope.loadItem = function (index) {
        loadItemIndex = index;
      };

      rootScope.next();

      expect(loadMoreIndex).toBe(3);
      expect(loadItemIndex).toBe(3);

    });
  });

});