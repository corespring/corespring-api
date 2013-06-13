angular.module('tagger.services')
    .factory('File',
    [ '$resource', 'ServiceLookup',
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
    [ '$resource', 'ServiceLookup', function ($resource, ServiceLookup) {

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
    .factory('ItemService', [ '$resource', 'ServiceLookup', '$http',
        function ($resource, ServiceLookup, $http) {

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

    //******Item Versioning*************//
//    ItemService.prototype.cloneAndIncrement = function(params, onSuccess, onError){
//        var url = ServiceLookup.getUrlFor('itemIncrement').replace(":id",params.id);
//        $http.get(url).success(onSuccess).error(onError)
//    }
//    ItemService.prototype.increment = function(params,onSuccess,onError){
//        var url = ServiceLookup.getUrlFor('itemIncrement').replace(":id",params.id)
//
//        var dto = ItemService.processor.createDTO(this);
//        $http.post(url,dto).success(function(resource){
//            ItemService.processor.processIncomingData(resource);
//            onSuccess(resource);
//        }).error(onError)
//    }
//    ItemService.prototype.currentItem = function(params,onSuccess,onError){
//        var url = "/api/v1/items/:id/current".replace(":id",params.id);
//        $http.get(url).success(onSuccess).error(onError)
//    }
    //****************************//

    ItemService.prototype.update = function (paramsObject, cb, onErrorCallback) {
        var idObject = angular.extend(paramsObject, {id:this.id});

        var dto = ItemService.processor.createDTO(this);
        return ItemService.update(idObject, dto, function(resource){
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
