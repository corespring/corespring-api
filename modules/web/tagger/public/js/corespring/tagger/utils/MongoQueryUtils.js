angular.module('tagger.services')
  .service('MongoQueryUtils',
  ['$log',
    function($log) {

      function MongoQueryUtils() {
        function fieldQuery(field, text) {
          var out = {};
          out[field] = {$regex: "\\b" + text, $options: "i"};
          return out;
        }

        this.fuzzyTextQuery = function(text, fields) {

          var query = {};
          if (!text) {
            return query;
          }

          query.$or = [];
          for (var i = 0; i < fields.length; i++) {
            query.$or.push(fieldQuery(fields[i], text));
          }
          return query;
        };

        this.and = function() {

          var out = {};
          out.$and = [];
          for (var i = 0; i < arguments.length; i++) {

            if (arguments[i]) {
              out.$and.push(arguments[i]);
            }
          }
          return out;
        };


        this.buildFilter = function(fields) {
          var filter = {};
          for (var i = 0; i < fields.length; i++) {
            filter[fields[i]] = 1;
          }
          return filter;
        };

        /**
         * Create mongo $in array
         * @return {{$in: Array}}
         */
        this.inArray = function(arr, key) {
          var out = [];
          for (var x = 0; x < arr.length; x++) {
            out.push(arr[x][key]);
          }
          return {$in: out};
        };

        this.notInArray = function(arr, key) {
          var out = [];
          for (var x = 0; x < arr.length; x++) {
            out.push(arr[x][key]);
          }
          return {$nin: out};
        };
      }

      return new MongoQueryUtils();
    }
  ]
);
