//var servicesModule = angular.module('tagger.services', ['ngResource']);

servicesModule
    .factory('ItemService', [ '$resource', 'ServiceLookup', 'AccessToken', function ($resource, ServiceLookup, AccessTokenService) {

    var ItemService = $resource(
        ServiceLookup.getUrlFor('items'),
        { },
        {
            update:{ method:'PUT'},
            count:{ method:'GET', isArray:false}
        }
    );

    ItemService.prototype.update = function (cb) {
        var idObject = {id:this._id.$oid};
        return ItemService.update(idObject, this, cb);
    };


    ItemService.prototype.destroy = function (cb) {
        return ItemService.remove({id:this._id.$oid}, cb);
    };

    return ItemService;
}]
);

