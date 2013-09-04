angular.module('tagger.services').factory('SearchService',
  function ($rootScope, ItemService) {

    /**
     * get item count and parse the results
     * @param queryText
     * @param callback - call back with the int value
     */
    var count = function (queryText, callback) {
      ItemService.count({q: queryText, c: true}, function (count) {
        if (count["count"] !== undefined) {
          var intCount = parseInt(count["count"]);
          callback(intCount);
        } else {
          throw "Error handling count response: " + count;
        }
      });
    };

    /** Private - query object builder
     *
     * @param searchParams
     * @param searchFields
     * @return {*}
     */
    var buildQueryObject = function (searchParams, searchFields) {
      function addIfTrue(query, value, key) {
        if (value) {
          query[key] = true;
        }
      }

      var query = mongoQuery.fuzzyTextQuery(searchParams.searchText, searchFields);

      var hasKey = function (element, key) {
        var foundElement = _.find(element, function (e) {
          return e.key == key;
        });
        return angular.isDefined(foundElement);

      };

      var isExactMatch = hasKey(searchParams.statuses, "exactMatch");
      var isSetup = hasKey(searchParams.statuses, "setup");
      var isTagged = hasKey(searchParams.statuses, "tagged");
      var isQaReview = hasKey(searchParams.statuses, "qaReview");
      var isStandardsAligned = hasKey(searchParams.statuses, "standardsAligned");

      if (isExactMatch) {
        query["workflow.setup"] = isSetup;
        query["workflow.tagged"] = isTagged;
        query["workflow.qaReview"] = isQaReview;
        query["workflow.standardsAligned"] = isStandardsAligned;

      } else {
        addIfTrue(query, isSetup, "workflow.setup");
        addIfTrue(query, isTagged, "workflow.tagged");
        addIfTrue(query, isQaReview, "workflow.qaReview");
        addIfTrue(query, isStandardsAligned, "workflow.standardsAligned");
      }
      if(searchParams.publishStatuses){
        var published = _.find(searchParams.publishStatuses,function(publishStatus){return publishStatus.key == "published"}) != undefined
        var draft = _.find(searchParams.publishStatuses,function(publishStatus){return publishStatus.key == "draft"}) != undefined
        if(published && !draft) query["published"] = true
        else if(!published && draft) query["published"] = false
      }

      /**
       * Returns either an exact value or a mongo $in array with a set of values.
       * If you pass in an array - you'll get an $in array,
       * if you pass in an object you'll get the objects value.
       * @param value
       * @param arrayKey
       * @return {*}
       */
      var objectOrArray = function (value, arrayKey) {
        if (value) {
          if (value.indexOf && value.length > 0) {
            return mongoQuery.inArray(value, arrayKey);
          } else {
            return value[arrayKey];
          }
        }
        else {
          return null;
        }
      };

      var gradeLevel = objectOrArray(searchParams.gradeLevel, "key");
      if (gradeLevel != null) {
        query["gradeLevel"] = gradeLevel;
      }

      var isOtherSelected = _.find(searchParams.itemType, function (e) {
        return e.label == "Other";
      });

      if (isOtherSelected) {
        // We need an inverse query - match everything except the item types that are not selected
        query["itemType"] = mongoQuery.notInArray(searchParams.notSelectedItemTypes, "label");
      } else {
        var itemType = objectOrArray(searchParams.itemType, "label");
        if (itemType != null) {
          query["itemType"] = itemType;
        }
      }

      var collectionId = objectOrArray(searchParams.collection, "id");
      if (collectionId != null) {
        query["collectionId"] = collectionId;
      }

      if (searchParams.contributor && searchParams.contributor.indexOf && searchParams.contributor.length > 0) {
        query["contributor"] = mongoQuery.inArray(searchParams.contributor, "name");
      }
      return query;
    };

    var mongoQuery = new com.corespring.mongo.MongoQuery();
    var searchService = {
      queryText: "",
      loaded: 0,
      limit: 50,
      isLastSearchRunning: false,
      resultCount: "",
      itemDataCollection: {},
      searchId: 0,
      resultFields: [
        'id',
        'title',
        'primarySubject',
        'gradeLevel',
        'itemType',
        'standards',
        'sourceUrl',
        'contributor',
        'author',
        'published',
        'collectionId'
      ],
      searchFields: [
        'title',
        'standards.dotNotation',
        'copyrightOwner',
        'contributor',
        'author',
        'published'
      ],

      /**
       * Build the complete ItemService query
       * @return an object that contains the following:
       * {
       *   q: a json string of the query
       *   l: an int indicating the limit
       *   sk: an int indicating the skip amount
       *   f: a json string indicating the fields to return
       *   sort: an optional sort json string
       * }
       */
      buildItemServiceQuery: function (addSkip) {

        var baseQuery = buildQueryObject(this.searchParams, this.searchFields);

        var out = {
          l: this.limit,
          q: JSON.stringify(baseQuery),
          f: JSON.stringify(mongoQuery.buildFilter(searchService.resultFields))
        };

        if (addSkip) {
          out.sk = this.loaded >= 0 ? this.loaded : -1
        }

        if (this.searchParams.sort) {
          out.sort = JSON.stringify(this.searchParams.sort);
        }
        return out;
      },

      /**
       * Perform a search using ItemService.query.
       * @param searchParams
       * @param resultHandler
       * @param errorHandler
       */
      search: function (searchParams, resultHandler, errorHandler) {


        /**
         * run the query.
         * We wrap this with an id so that we can disregard the results of stale queries.
         * @param id
         */
        var run = function (id) {

          var query = this.buildItemServiceQuery(false);

          /**
           * Query success callback
           * @param data
           */
          var onQuerySuccess = function (data) {

            if (id != searchService.searchId) {
              return;
            }

            this.itemDataCollection = data;
            this.loaded += this.limit;
            resultHandler(data);

            var onCountSuccess = function (resultCount) {
              this.resultCount = resultCount;
              $rootScope.$broadcast('onSearchCountComplete', resultCount);
              $rootScope.$broadcast('onNetworkComplete');
            };

            count(query.q, angular.bind(this, onCountSuccess));
          };

          ItemService.query(query, angular.bind(this, onQuerySuccess), errorHandler)
        };

        this.searchParams = searchParams;
        this.searchId = new Date().getTime();
        this.loaded = 0;
        $rootScope.$broadcast('onNetworkLoading');

        run.apply(this, [this.searchId]);
      },

      /**
       * Using the existing search params - load more items
       * @param resultHandler
       */
      loadMore: function (resultHandler) {

        if (this.loaded >= this.resultCount) {
          return;
        }

        $rootScope.$broadcast('onNetworkLoading');

        if (this.isLastSearchRunning) {
          return;
        }

        this.isLastSearchRunning = true;

        var query = this.buildItemServiceQuery(true);

        var onSuccess = function (data) {
          this.itemDataCollection = this.itemDataCollection.concat(data);
          resultHandler(data);
          this.isLastSearchRunning = false;
          $rootScope.$broadcast('onNetworkComplete');
          this.loaded += this.limit;
        };

        ItemService.query(query, angular.bind(this, onSuccess));
      },


      resetDataCollection: function () {
        searchService.itemDataCollection = [];
      }
    };
    return searchService;
  });
