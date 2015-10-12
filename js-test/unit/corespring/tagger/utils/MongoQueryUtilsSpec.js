'use strict';

describe('MongoQuery', function() {

  var mongoQueryUtils;

  beforeEach(module('tagger.services'));

  beforeEach(inject(function(MongoQueryUtils) {
    mongoQueryUtils = MongoQueryUtils;
  }));


  describe('behaviour', function() {

    it('builds a fuzzy text query', function() {

      var queryObject = mongoQueryUtils.fuzzyTextQuery("ed", ["name", "title"]);
      expect(queryObject.$or.length).toBe(2);

      expect(queryObject).toEqual({
        $or: [
          {name: {$regex: "\\bed", $options: "i"}},
          {title: {$regex: "\\bed", $options: "i"}}
        ]
      });
    });

    it('builds and array', function() {
      var queryObject = mongoQueryUtils.and({a: "a"}, {b: "b"}, {c: "c"});
      expect(queryObject).toEqual({$and: [{a: "a"}, {b: "b"}, {c: "c"}]})
    });
  });
});
