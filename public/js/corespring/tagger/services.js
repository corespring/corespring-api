'use strict';

window.servicesModule = ( window.servicesModule || angular.module('tagger.services', ['ngResource']));



// module for the mongo collection 'itemdata' resource on mongolab
/*
window.servicesModule
    .factory('ItemData', [ '$resource', 'ServiceLookup', function ($resource, ServiceLookup) {

    var ItemData = $resource(
        ServiceLookup.getUrlFor('items') + "/:id",
        //TODO: hide this key
        { apiKey:'4fbe6747e4b083e37574238b' },
        {
            update:{ method:'PUT' },
            count:{method:'GET', isArray:false}
        }
    );

    ItemData.processor = new com.corespring.model.ItemDataProcessor();

    ItemData.createWorkflowObject = ItemData.processor.createWorkflowObject;

    ItemData.prototype.update = function (cb) {
        var idObject = {id:this._id.$oid};
        var dto = ItemData.processor.createDTO(this);
        return ItemData.update(idObject, dto, cb);
    };

    //The currently retrieved item
    ItemData._currentItemData = null;
    //The current Item id
    ItemData._currentItemId = null;
    //stash the default get implementation
    ItemData.angularGet = ItemData.get;

    ItemData._getInProgress = false;

    ItemData._getCallbacks = {};

    ItemData.get = function (object, callback) {

        if (ItemData._getInProgress) {

            if (ItemData._getCallbacks[object.id] == undefined) {
                ItemData._getCallbacks[object.id] = [];
            }
            ItemData._getCallbacks[object.id].push(callback);
            return;
        }
        if (object.id === ItemData._currentItemId) {
            callback(ItemData._currentItemData);
        }
        else {
            ItemData._getCallbacks = {};
            ItemData._currentItemId = null;
            ItemData._getInProgress = true;

            ItemData.angularGet(object, function resourceLoaded(resource) {
                ItemData._curentItemId = resource._id.$oid;
                ItemData._getInProgress = false;
                ItemData.processor.processIncomingData(resource);
                ItemData._currentItemData = new ItemData(resource);
                callback(ItemData._currentItemData);

                var pendingCallbacks = ItemData._getCallbacks[ItemData._curentItemId];

                if (pendingCallbacks === undefined) {
                    return;
                }

                for (var i = 0; i < pendingCallbacks.length; i++) {
                    var pendingCallback = pendingCallbacks[i];
                    pendingCallback(ItemData._currentItemData);
                }
            });
        }
    };

    ItemData.prototype.destroy = function (cb) {
        return ItemData.remove({id:this._id.$oid}, cb);
    };

    return ItemData;
}]
);*/

// module for the mongo collection 'Collection' resource on mongolab
// 
window.servicesModule
    .factory('Collection', [ '$resource', 'ServiceLookup', function ($resource, ServiceLookup) {
    var Collection = $resource(
        ServiceLookup.getUrlFor('collection') + '/:id',
        { apiKey:'4fbe6747e4b083e37574238b' },
        {
            update:{ method:'PUT' },
            count:{method:'GET', isArray:false}
        }
    );

    Collection.prototype.update = function (cb) {
        return Collection.update(
            {id:this._id.$oid},
            angular.extend(
                {},
                this,
                {
                    _id:undefined
                }),
            cb);
    };

    Collection.prototype.destroy = function (cb) {
        return Collection.remove({id:this._id.$oid}, cb);
    };
    return Collection;
}]
);


/*
 * module for the mongo collection 'Collection' resource on mongolab
 */
window.servicesModule
    .factory('CcStandard', ['$resource', 'ServiceLookup', function ($resource, ServiceLookup) {
    var CcStandard = $resource(
        ServiceLookup.getUrlFor('standards') + '/:id',
        { apiKey:'4fbe6747e4b083e37574238b' },
        {
            update:{ method:'PUT' },
            count:{method:'GET', isArray:false}
        }
    );

    CcStandard.prototype.update = function (cb) {
        return CcStandard.update(
            {id:this._id.$oid},
            angular.extend(
                {},
                this,
                {
                    _id:undefined
                }),
            cb);
    };

    CcStandard.prototype.destroy = function (cb) {
        return CcStandard.remove({id:this._id.$oid}, cb);
    };
    return CcStandard;
}]
);

/*
 * module for the mongo collection 'subject' resource on mongolab
 */
window.servicesModule
    .factory('Subject', ['$resource', 'ServiceLookup', function ($resource, ServiceLookup) {
    var Subject = $resource(
        ServiceLookup.getUrlFor('subject') + '/:id',
        { apiKey:'4fbe6747e4b083e37574238b' },
        {
            update:{ method:'PUT' },
            count:{method:'GET', isArray:false}
        }
    );

    Subject.prototype.update = function (cb) {
        return Subject.update(
            {id:this._id.$oid},
            angular.extend(
                {},
                this,
                {
                    _id:undefined
                }),
            cb);
    };

    Subject.prototype.destroy = function (cb) {
        return Subject.remove({id:this._id.$oid}, cb);
    };
    return Subject;
}]
);

window.servicesModule.factory('searchService', function ($rootScope, ItemData) {

    var mongoQuery = new com.corespring.mongo.MongoQuery();

    var searchService = {
        queryText:"",
        // already loaded items will be skipped when query is executed
        loaded:0,
        // this is the limit for how many items to load at one time
        limit:20,
        // flag to indicate if search is running currently
        isLastSearchRunning:false,
        resultCount:"",
        itemDataCollection:{},
        resultFields:['originId', 'title', 'primarySubject', 'gradeLevel', 'itemType', 'itemTypeOther', 'standards'],
        searchFields:[
            'originId',
            'title',
            'primarySubject',
            'copyrightOwner',
            'contributor',
            'author',
            'standards.standard',
            'standards.dotNotation',
            'standards.subject',
            'standards.category',
            'standards.subCategory'], // fields to search with text query
        /**
         * function for searching the items on mongo.
         */
        search:function (searchParams, resultHandler) {
            this.searchParams = searchParams;
            $rootScope.$broadcast('onNetworkLoading');
            this.loaded = 0;

            var query = this.buildQueryObject(searchParams, this.searchFields, this.resultFields);

            ItemData.query(query, function (data) {
                $rootScope.$broadcast('onNetworkComplete');	// see networkProgressDirective
                //TODO - is this necessary?
                searchService.itemDataCollection = data;
                resultHandler(data);
                searchService.count(query.q, function (resultCount) {
                    searchService.resultCount = parseInt(resultCount);
                    $rootScope.$broadcast('onSearchCountComplete', resultCount);
                });
            });

            this.loaded = this.loaded + this.limit;
        },

        /**
         * Build a query object in the form of: {q: .., l: .. , f: .., sk: .. }
         * @param searchParams
         * @param searchFields
         * @param resultFields
         * @return {Object}
         */
        buildQueryObject:function (searchParams, searchFields, resultFields) {

            /**
             * Only add the workflow param if its true as some items don't even define it.
             * @param query
             * @param value
             * @param key
             */
            function addIfTrue(query, value, key) {
                if (value) {
                    query[key] = true;
                }
            }

            var query = mongoQuery.fuzzyTextQuery(searchParams.searchText, searchFields);

            addIfTrue(query, searchParams.setup, "workflow.setup");
            addIfTrue(query, searchParams.tagged, "workflow.tagged");
            addIfTrue(query, searchParams.qaReview, "workflow.qaReview");
            addIfTrue(query, searchParams.standardsAligned, "workflow.standardsAligned");

            if (searchParams.collection && searchParams.collection.name) {
                query.collection = searchParams.collection.name;
            }

            return {
                q:JSON.stringify(query),
                l:this.limit,
                f:JSON.stringify(mongoQuery.buildFilter(resultFields)),
                sk:this.loaded >= 0 ? this.loaded : -1};
        },

        /**
         * Loads more items for infinite scroll
         */
        loadMore:function (resultHandler) {

            if (this.loaded >= this.resultCount) {
                return;
            }

            $rootScope.$broadcast('onNetworkLoading');

            //TODO try / catch here...
            if (this.isLastSearchRunning) {
                console.log("last request not done yet");
                return;
            }

            this.isLastSearchRunning = true; // set flag

            var query = this.buildQueryObject(this.searchParams, this.searchFields, this.resultFields);

            ItemData.query(query, function (newItems) {
                searchService.itemDataCollection = searchService.itemDataCollection.concat(newItems);
                resultHandler(); // call the result handler so it can update the model
                searchService.isLastSearchRunning = false; // reset flag
                $rootScope.$broadcast('onNetworkComplete');
            });

            this.loaded = this.loaded + this.limit;
        },

        /**
         * Get a count of how many results are available for the query
         * @param queryText - a json string
         * @param callback - callback function
         */
        count:function (queryText, callback) {
            ItemData.count({q:queryText, c:true}, function (count) {
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
