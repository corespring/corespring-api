window.servicesModule = ( window.servicesModule || angular.module('tagger.services', ['ngResource']));

servicesModule
    .factory('ItemService', [ '$resource', 'ServiceLookup', 'AccessToken', function ($resource, ServiceLookup, AccessTokenService) {

    var ItemService = $resource(
        ServiceLookup.getUrlFor('items'),
        { },
        {

            update:{ method:'PUT'},
            //Enable for mock services
            //query: { method: 'GET', params:{id:'list.json'}, isArray: true},
            query: { method: 'GET', isArray: true},
            count:{ method:'GET', isArray:false}
        }
    );

    ItemService.prototype.update = function (paramsObject, cb) {
        var idObject = angular.extend( paramsObject, {id:this.id});
        return ItemService.update(idObject, this, cb);
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

    ItemService._getInProgress = false;

    ItemService._getCallbacks = {};

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

            ItemService.angularGet(object, function resourceLoaded(resource) {
                ItemService._curentItemId = resource.id;
                ItemService._getInProgress = false;
                ItemService.processor.processIncomingData(resource);
                ItemService._currentItemService = new ItemService(resource);
                callback(ItemService._currentItemService);

                var pendingCallbacks = ItemService._getCallbacks[ItemService._curentItemId];

                if (pendingCallbacks === undefined) {
                    return;
                }
                _.forEach(pendingCallbacks, function(pc){
                   pc(ItemService._currentItemService);
                });
            });
        }
    };

    ItemService.prototype.destroy = function (cb) {
        return ItemService.remove({id:this._id.$oid}, cb);
    };

    return ItemService;
}]
);

