'use strict';

window.servicesModule = ( window.servicesModule || angular.module('tagger.services', ['ngResource']));

/**
 * Lookup for all ajax services.
 */
window.servicesModule
    .factory('ServiceLookup', function () {

        var ServiceLookup = function () {

            this.services = {
                getAccessToken: '/web/access_token',
                items: '/api/v1/items/:id',
                //items: '/assets/mock-json/:id',
                previewFile: '/web/runner/{key}',
                uploadSupportingMaterial: '/api/v1/items/{itemId}/materials/{fileName}',
                deleteSupportingMaterial: '/api/v1/items/{itemId}/materials/{fileName}',
                standardsTree: '/assets/web/standards_tree.json',
                standards:  '/api/v1/field_values/cc-standard',
                subject:    '/api/v1/field_values/subject',
                collection: '/api/v1/collections',
                uploadFile:'/tagger/upload/{itemId}/{fileName}',
                viewFile:'/tagger/files/{itemId}/{fileName}',
                deleteFile:'/tagger/delete/{itemId}/{fileName}'
            };
        };

        ServiceLookup.prototype.getUrlFor = function (name) {
            if (this.services.hasOwnProperty(name)) {
                return this.services[name];
            }
            throw "Can't find service for name: " + name;
        };

        return new ServiceLookup();
    });


com.corespring.model = (com.corespring.model || {});
/**
 * Manipulates the incoming and outgoing data.
 * Incoming - so it works for the controller.
 * Outgoing - so any extraneaous data is removed and and updates made by the user
 * are in the correct model format for saving.
 * @constructor
 */
com.corespring.model.ItemDataProcessor = function () {


    /**
     * Create Data transfer object to send across the wire.
     * @param itemData - the item data model
     */
    this.createDTO = function (itemData) {
        var dto = {};
        var processedData = { _id:undefined };
        processedData.gradeLevel = this.buildDtoKeyArray(itemData.$gradeLevelDataProvider);
        processedData.reviewsPassed = this.buildDtoKeyArray(itemData.$reviewsPassedDataProvider);
        angular.extend(dto, itemData, processedData);

        this.processOids(dto, false);
        return dto;
    };

    this.arraysThatNeedProcessing = ["primarySubject", "relatedSubject", "standards"];
    /**
     * look for any objects that are objects from another collection
     * aka they have an _id.$oid property.
     * hang onto the id but move it to refId instead.
     * @param data
     */
    this.processOids = function (data, processUnderscoreId) {

        function _processIdObject(objectWithId) {

            if (objectWithId == undefined || objectWithId == null) {
                return;
            }

            if (objectWithId.hasOwnProperty("_id") && objectWithId._id != null && objectWithId._id.$oid !== undefined) {
                objectWithId.refId = objectWithId._id.$oid;
                delete objectWithId._id
            }
            else {
                if (objectWithId.refId != null) {
                    //it already processed
                } else {
                    console.warn("couldn't process object: " + objectWithId);
                }
            }
        }

        _processIdObject(data["primarySubject"]);
        _processIdObject(data["relatedSubject"]);

        if (data["standards"] !== undefined) {
            for (var i = 0; i < data["standards"].length; i++) {
                _processIdObject(data["standards"][i]);
            }

        }
    };

    /**
     *
     * @param dataProvider - the ng style dataprovider:
     * [ {state: { selected: true, key: "blah", label: "blah" }}, ...]
     * @return {Array} of values that were true eg: ["Blah"]
     */
    this.buildDtoKeyArray = function (dataProvider) {

        var out = [];
        for (var i = 0; i < dataProvider.length; i++) {
            var def = dataProvider[i];
            if (def.state.selected) {
                out.push(def.state.key);
            }
        }
        return out;
    };


    /**
     * Before we return the resource to the controller,
     * build out the object structure. Also map any data that needs to be mapped
     * to allow the controller and bindings to function.
     * @param resource
     */
    this.processIncomingData = function (item) {

        item.$defaults = this.$defaults;

        if (item.files == null) {
            item.files = [];
        }

        if (item.standards == null) {
            item.standards = [];
        }

        if (item.primaryStandard != null) {
            throw "Data model contains a 'primaryStandard' - need to move these to the 'standards' array";
        }

        if (!item.workflow) {
            item.workflow = this.createWorkflowObject();
        }

        /**
         * Dataproviders are prepended with a $ so that angular doesn't send them across the wire.
         * @type {Array}
         */
        item.$gradeLevelDataProvider = this.buildNgDataProvider(this.$defaults.gradeLevels, item.gradeLevel);
        item.$reviewsPassedDataProvider = this.buildNgDataProvider(this.$defaults.reviewsPassed, item.reviewsPassed);
        item.$priorUseDataProvider = _.map(window.fieldValues.priorUses, function (p) { return p.key });
        item.$credentialsDataProvider = _.map(window.fieldValues.credentials, function(c){return c.key});
        item.$licenseTypeDataProvider = _.map(window.fieldValues.licenseTypes, function(c){return c.key});
        item.$bloomsTaxonomyDataProvider = _.map(window.fieldValues.bloomsTaxonomy, function(c){return c.key});
        item.$itemTypeDataProvider = window.fieldValues.itemTypes;

        if (!item.keySkills) {
            item.keySkills = [];
        }
    };

    this.$defaults = {
        keySkills:_.map(window.fieldValues.keySkills, function (k) {
            return {header:k.key, list:k.value}
        }),
        reviewsPassed:_.map(window.fieldValues.reviewsPassed, function (g) {
            return {key:g.key, label:g.value}
        }),
        gradeLevels:_.map(window.fieldValues.gradeLevels, function (g) {
            return {key:g.key, label:g.value}
        })
    };

    this.createWorkflowObject = function () {
        return {
            setup:false,
            tagged:false,
            standardsAligned:false,
            qaReview:false
        };
    };

    /**
     * Take a simple array of values like so: ['A','B','C',...]
     * and convert it to: { {state:{ key: A, selected: true, label: x}}, ... }
     *
     * This object format is necessary if you with to build repeaters off the datamodel.
     * @see https://github.com/angular/angular.js/issues/686
     *
     * This may change or be fixed which will allow this to be simplified.
     * @param defaults - an array of defaults that drive the ui
     * @param dtoArray - the values saved in the datamodel
     * @return {Array}
     */
    this.buildNgDataProvider = function (defaults, dtoArray) {

        function getValueFromModel(key, dtoArray) {

            if (dtoArray === undefined) {
                return false;
            }

            /**
             * @deprecated
             * Capture old data model that has moved from a single string value to an
             * array of string values.
             */
            if (typeof(dtoArray) == "string") {
                console.warn("deprecated: Captured simple string value");
                return key === dtoArray;
            }

            if (dtoArray.indexOf !== undefined) {
                var index = dtoArray.indexOf(key);
                return index != -1
            }
            else {
                /**
                 * @deprecated - this is to support a legacy data format - to be removed
                 */
                console.warn("deprecated: using an old data model format");
                var item = (dtoArray[key] || dtoArray[parseFloat(key)]);
                return item;
            }
            return false;
        }

        var out = [];
        for (var x = 0; x < defaults.length; x++) {
            var definition = defaults[x];
            var selected = getValueFromModel(definition.key, dtoArray);
            var item = {state:{}};
            angular.extend(item.state, definition, {selected:selected});
            out.push(item);
        }
        return out;
    };

    this.buildObjectFromTokens = function (tokens, initialValue) {
        var out = {};
        var tokensArray = tokens.split(",");
        for (var i = 0; i < tokensArray.length; i++) {
            out[ tokensArray[i] ] = initialValue;
        }
        return out;
    };
}
;


// module for the mongo collection 'itemdata' resource on mongolab
// 
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

    /**
     * Several controllers within the edit context would like the itemData object.
     * The service keeps an instance stored - and if the controller asks for the same item
     * as is currently loaded it returns it.
     * @param object
     * @param callback
     */
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
);

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
