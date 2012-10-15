'use strict';

describe('MongoQuery', function () {

    var query;

    beforeEach(function () {
        query = new com.corespring.mongo.MongoQuery();
    });

    describe('behaviour',function(){

        it('builds a fuzzy text query', function(){

            var queryObject = query.fuzzyTextQuery("ed", ["name", "title"]);
            expect(queryObject.$or.length).toBe(2);

            expect(queryObject).toEqual( { $or: [
                { name: { $regex: "\\bed", $options: "i" } },
                { title: { $regex: "\\bed", $options: "i" } }
            ]});
        });

        it('builds and array', function(){
            var queryObject = query.and({a:"a"}, {b: "b"}, {c: "c"});
            expect(queryObject).toEqual({ $and: [ {a:"a"}, {b: "b"}, {c: "c"}] })
        });
    });
});
