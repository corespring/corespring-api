try{
    angular.module('tagger.services')
}
catch(e) {
    angular.module('tagger.services', ['ngResource']);
    window.servicesModule = ( window.servicesModule || angular.module('tagger.services'));
}

servicesModule
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


servicesModule
    .factory('SupportingMaterial',
    [ '$resource', 'ServiceLookup', 'AccessToken', function ($resource, ServiceLookup) {

        return $resource(
            ServiceLookup.getUrlFor('materials') + '/:resourceName',
            {},
            {
                query:{ method:'GET', isArray:true},
                delete:{ method:'DELETE', isArray:false}
            }
        );
    }]);



angular.module('tagger.services')
    .factory('ItemService', [ '$resource', 'ServiceLookup', 'AccessToken', function ($resource, ServiceLookup, AccessTokenService) {

    var ItemService = $resource(
        ServiceLookup.getUrlFor('items'),
        { },
        {

            update:{ method:'PUT'},
            //Enable for mock services
            //query: { method: 'GET', params:{id:'list.json'}, isArray: true},
            query:{ method:'GET', isArray:true},
            count:{ method:'GET', isArray:false}
        }
    );

    ItemService.prototype.update = function (paramsObject, cb) {
        var idObject = angular.extend(paramsObject, {id:this.id});

        var copy = {};
        angular.copy(this, copy);
        copy.id = null;
        delete copy.id;
        delete copy.collectionId;

        /**
         * We need to only send the ids for items instead of embedded objects
         * @param item
         * @return {*}
         */
        function convertEmbeddedToOid(item){
            if(!item || !item.id){
                throw "No item sent to convertEmbeddedToOid"
            }
            return  item.id;
        }

        if(copy.primarySubject){
            copy.primarySubject = convertEmbeddedToOid(copy.primarySubject);
        }

        if(copy.relatedSubject){
            copy.relatedSubject = convertEmbeddedToOid(copy.relatedSubject);
        }
        copy.standards = _.map(copy.standards, convertEmbeddedToOid);

        return ItemService.update(idObject, copy, function(resource){
            ItemService.processor.processIncomingData(resource);
            cb(resource)
        });
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

