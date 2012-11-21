angular.module('tagger.services')
    .factory('File',
    [ '$resource', 'ServiceLookup', 'AccessToken',
        function ($resource, ServiceLookup) {
            return $resource(
                ServiceLookup.getUrlFor('file') + '/:filename',
                {},
                {
                   upload: { method: 'POST'}
                }
            );
        }
    ]);


angular.module('tagger.services')
    .factory('SupportingMaterial',
    [ '$resource', 'ServiceLookup', 'AccessToken', function ($resource, ServiceLookup) {

        return $resource(
            ServiceLookup.getUrlFor('materials') + '/:resourceName',
            {},
            {
                query:{ method:'GET', isArray:true},
                "delete":{ method:'DELETE', isArray:false}
            }
        );
    }]);



angular.module('tagger.services')
    .factory('ItemService', [ '$resource', 'ServiceLookup', 'AccessToken', '$http',
        function ($resource, ServiceLookup, AccessTokenService, $http) {

    var ItemService = $resource(
        ServiceLookup.getUrlFor('items'),
        { },
        {
            update:{ method:'PUT'},
            query:{ method:'GET', isArray:true},
            count:{ method:'GET', isArray:false}
        }
    );

    ItemService.prototype.clone = function( params, onSuccess, onError) {
        var url = "/api/v1/items/:id".replace(":id", params.id);
        var successCallback = function(data){
            onSuccess(data);
        };

        $http.post(url, {})
            .success(successCallback)
            .error(onError);
    };

    ItemService.prototype.update = function (paramsObject, cb, onErrorCallback) {
        var idObject = angular.extend(paramsObject, {id:this.id});

        var copy = ItemService.processor.createDTO(this);
        copy.id = null;
        return ItemService.update(idObject, copy, function(resource){
            ItemService.processor.processIncomingData(resource);
            cb(resource);
        }, onErrorCallback);
    };


    ItemService.prototype.destroy = function (cb) {
        return ItemService.remove({id:this.id}, cb);
    };


    ItemService.processor = new com.corespring.model.ItemDataProcessor();
    ItemService.createWorkflowObject = ItemService.processor.createWorkflowObject;


    //The currently retrieved item
    ItemService._currentItemService = null;
    //The current Item id
    ItemService._currentItemId = null;
    //stash the default get implementation
    ItemService.angularGet = ItemService.get;
    //ItemService.angularUpdate = ItemService.update;

    ItemService._getInProgress = false;

    ItemService._getCallbacks = {};

    ItemService.resourceLoaded = function(callback, resource){
        ItemService._curentItemId = resource.id;
        ItemService._getInProgress = false;
        ItemService.processor.processIncomingData(resource);
        ItemService._currentItemService = new ItemService(resource);
        callback(ItemService._currentItemService);

        var pendingCallbacks = ItemService._getCallbacks[ItemService._curentItemId];

        if (pendingCallbacks === undefined) {
            return;
        }
        _.forEach(pendingCallbacks, function (pc) {
            pc(ItemService._currentItemService);
        });

    };

    //ItemService.update = function(object,callback){
    //    ItemService.angularUpdate(object, function(resource){ ItemService.resourceLoaded(callback, resource)});
    //};

    /**
     * Several controllers within the edit context would like the itemData object.
     * The service keeps an instance stored - and if the controller asks for the same item
     * as is currently loaded it returns it.
     * @param object
     * @param callback
     */
    ItemService.get = function (object, callback) {

        if (ItemService._getInProgress) {

            if (ItemService._getCallbacks[object.id] == undefined) {
                ItemService._getCallbacks[object.id] = [];
            }
            ItemService._getCallbacks[object.id].push(callback);
            return;
        }
        if (object.id === ItemService._currentItemId) {
            callback(ItemService._currentItemService);
        }
        else {
            ItemService._getCallbacks = {};
            ItemService._currentItemId = null;
            ItemService._getInProgress = true;
            ItemService.angularGet(object, function(resource){ ItemService.resourceLoaded(callback, resource) } );
        }
    };

    ItemService.prototype.destroy = function (cb) {
        return ItemService.remove({id:this._id.$oid}, cb);
    };

    return ItemService;
}]
);

angular.module('tagger.services').factory('SearchService',
    function ($rootScope, ItemService, AccessToken) {


    var mongoQuery = new com.corespring.mongo.MongoQuery();
    var searchService = {
        queryText:"",
        loaded:0,
        limit:50,
        isLastSearchRunning:false,
        resultCount:"",
        itemDataCollection:{},
        searchId:0,
        resultFields:['originId', 'title', 'primarySubject', 'gradeLevel', 'itemType','standards', 'subjects'],
        searchFields:[
            'originId',
            'title',
            'contributorDetails.copyright.owner',
            'contributorDetails.contributor',
            'contributorDetails.author'
        ],

        search:function (searchParams, resultHandler) {
            this.searchParams = searchParams;
            $rootScope.$broadcast('onNetworkLoading');
            this.loaded = 0;

            var query = this.buildQueryObject(searchParams, this.searchFields);
            searchService.searchId = new Date().getTime();
            (function(id) {
                ItemService.query({
                        l: searchService.limit,
                        q:JSON.stringify(query),
                        f:JSON.stringify(mongoQuery.buildFilter(searchService.resultFields))
                    }, function (data) {
                        if (id != searchService.searchId) {
                            return;
                        }
                        searchService.itemDataCollection = data;
                        resultHandler(data);
                        searchService.count(JSON.stringify(query), function (resultCount) {
                            searchService.resultCount = parseInt(resultCount);
                            $rootScope.$broadcast('onSearchCountComplete', resultCount);
                        });
                    }
                );
            })(searchService.searchId);

            this.loaded = this.loaded + this.limit;
        },

        buildQueryObject:function (searchParams, searchFields, resultFields) {
            function addIfTrue(query, value, key) {
                if (value) {
                    query[key] = true;
                }
            }

            var query = mongoQuery.fuzzyTextQuery(searchParams.searchText, searchFields);

            if( searchParams.exactMatch ){
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

            if (searchParams.collection && searchParams.collection.id ) {
                query.collectionId = searchParams.collection.id;
            }

            return query;
        },

        loadMore:function (resultHandler) {

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
                    q:JSON.stringify(query),
                    f:JSON.stringify(mongoQuery.buildFilter(this.resultFields)),
                    sk:this.loaded >= 0 ? this.loaded : -1
                }, function (data) {
                    searchService.itemDataCollection = searchService.itemDataCollection.concat(data);
                    resultHandler(data);
                    searchService.isLastSearchRunning = false; // reset flag
                    $rootScope.$broadcast('onNetworkComplete');
                }
            );

            this.loaded = this.loaded + this.limit;
        },

        count:function (queryText, callback) {
           ItemService.count({q:queryText, c:true}, function (count) {
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
       },


        resetDataCollection:function () {
            searchService.itemDataCollection = [];
        }
    };
    return searchService;
});
