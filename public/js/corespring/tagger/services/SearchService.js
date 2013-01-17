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
                'taskInfo.title',
                'taskInfo.subjects.primary',
                'taskInfo.gradeLevel',
                'taskInfo.itemType',
                'standards',
                'contributorDetails.sourceUrl',
                'contributorDetails.contributor',
                'contributorDetails.author',
                'subjects'],
            searchFields: [
                'originId',
                'taskInfo.title',
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
                        function onError(data) {
                            console.warn("error occurred: " + data);
                            if (errorHandler) errorHandler(data);
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

                var hasKey = function (element, key) {
                    var foundElement = _.find(element, function (e) {
                        return e.key == key;
                    });
                    return angular.isDefined(foundElement);

                };

                var isExactMatch = searchParams.exactMatch || hasKey(searchParams.statuses, "exactMatch");
                var isSetup = searchParams.setup || hasKey(searchParams.statuses, "setup");
                var isTagged = searchParams.tagged || hasKey(searchParams.statuses, "tagged");
                var isQaReview = searchParams.qaReview || hasKey(searchParams.statuses, "qaReview");
                var isStandardsAligned = searchParams.standardsAligned || hasKey(searchParams.statuses, "standardsAligned");

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
                        query['taskInfo.gradeLevel'] = inArray(searchParams.gradeLevel, "key");
                    } else {
                        query['taskInfo.gradeLevel'] = searchParams.gradeLevel.key;
                    }
                }

                if (searchParams.itemType) {
                    console.log("Item Type: ", searchParams.itemType);
                    if (searchParams.itemType.indexOf && searchParams.itemType.length > 0) {
                        query['taskInfo.itemType'] = inArray(searchParams.itemType, "label");
                    } else {
                        query['taskInfo.itemType'] = searchParams.itemType.label;
                    }
                }

                if (searchParams.collection) {
                    if (searchParams.collection.indexOf && searchParams.collection.length > 0) {
                        query.collectionId = inArray(searchParams.collection, "id");
                    } else {
                        query.collectionId = searchParams.collection.id;
                    }
                }

                if (searchParams.contributor && searchParams.contributor.indexOf && searchParams.contributor.length > 0) {
                    query["contributorDetails.contributor"] = inArray(searchParams.contributor, "name");
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
