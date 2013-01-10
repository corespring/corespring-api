angular.module('tagger.services').factory('SearchService',
  function ($rootScope, ItemService) {


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
        'originId',
        'title',
        'primarySubject',
        'gradeLevel',
        'itemType',
        'standards',
        'contributorDetails.sourceUrl',
        'contributorDetails.contributor',
        'contributorDetails.author',
        'subjects'],
      searchFields: [
        'originId',
        'title',
        'standards',
        'contributorDetails.copyright.owner',
        'contributorDetails.contributor',
        'contributorDetails.author'
      ],
      

      search: function (searchParams, resultHandler, errorHandler) {
        this.searchParams = searchParams;
        $rootScope.$broadcast('onNetworkLoading');
        this.loaded = 0;

        //Private count function
        var count = function (queryText, callback) {
          ItemService.count({q: queryText, c: true}, function (count) {
            // angular treats a raw string like a json object and copies it (see copy() method in angular)
            // this mean "21" will come back as {0:"2", 1:"1"}
            // need to unpack this here....
            var unpackedCount = "";
            for (var key in count) {
              if (count.hasOwnProperty(key))
                unpackedCount += count[key];
            }
            callback(unpackedCount);
          });
        };

        var query = this.buildQueryObject(searchParams, this.searchFields);
        searchService.searchId = new Date().getTime();
        var executeQuery = function (id) {
          ItemService.query({
              l: searchService.limit,
              q: JSON.stringify(query),
              f: JSON.stringify(mongoQuery.buildFilter(searchService.resultFields))
            },
            function (data) {
              if (id != searchService.searchId) {
                return;
              }
              searchService.itemDataCollection = data;
              resultHandler(data);
              count(JSON.stringify(query), function (resultCount) {
                searchService.resultCount = parseInt(resultCount);
                $rootScope.$broadcast('onSearchCountComplete', resultCount);
                $rootScope.$broadcast('onNetworkComplete');
              });
            },
            function onError(data){
              console.warn("error occurred: " + data);
              if(errorHandler) errorHandler(data);
            }
          );
        };
        executeQuery(searchService.searchId);

        this.loaded = this.loaded + this.limit;
      },

      buildQueryObject: function (searchParams, searchFields, resultFields) {
        function addIfTrue(query, value, key) {
          if (value) {
            query[key] = true;
          }
        }

        var query = mongoQuery.fuzzyTextQuery(searchParams.searchText, searchFields);

        if (searchParams.exactMatch) {
          query["workflow.setup"] = searchParams.setup;
          query["workflow.tagged"] = searchParams.tagged;
          query["workflow.qaReview"] = searchParams.qaReview;
          query["workflow.standardsAligned"] = searchParams.standardsAligned;

        } else {
          addIfTrue(query, searchParams.setup, "workflow.setup");
          addIfTrue(query, searchParams.tagged, "workflow.tagged");
          addIfTrue(query, searchParams.qaReview, "workflow.qaReview");
          addIfTrue(query, searchParams.standardsAligned, "workflow.standardsAligned");
        }

        /**
         * Create mongo $in array
         * @return {{$in: Array}}
         */
        var inArray = function (arr, key) {
          var out = [];
          for (var x = 0; x < arr.length; x++) {
            out.push(arr[x][key]);
          }
          return { $in: out};
        };

        if (searchParams.gradeLevel) {
          if (searchParams.gradeLevel.indexOf && searchParams.gradeLevel.length > 0) {
            query.gradeLevel = inArray(searchParams.gradeLevel, "key");
          } else {
            query.gradeLevel = searchParams.gradeLevel.key;
          }
        }

        if (searchParams.collection) {
          if (searchParams.collection.indexOf && searchParams.collection.length > 0) {
            query.collectionId = inArray(searchParams.collection, "id");
          } else {
            query.collectionId = searchParams.collection.id;
          }
        }
        return query;
      },

      loadMore: function (resultHandler) {

        if (this.loaded >= this.resultCount) {
          return;
        }

        $rootScope.$broadcast('onNetworkLoading');

        if (this.isLastSearchRunning) {
          console.log("last request not done yet");
          return;
        }

        this.isLastSearchRunning = true; // set flag

        var query = this.buildQueryObject(this.searchParams, this.searchFields, this.resultFields);

        ItemService.query({
            l: this.limit,
            q: JSON.stringify(query),
            f: JSON.stringify(mongoQuery.buildFilter(this.resultFields)),
            sk: this.loaded >= 0 ? this.loaded : -1
          }, function (data) {
            searchService.itemDataCollection = searchService.itemDataCollection.concat(data);
            resultHandler(data);
            searchService.isLastSearchRunning = false; // reset flag
            $rootScope.$broadcast('onNetworkComplete');
          }
        );

        this.loaded = this.loaded + this.limit;
      },


      resetDataCollection: function () {
        searchService.itemDataCollection = [];
      }
    };
    return searchService;
  });
