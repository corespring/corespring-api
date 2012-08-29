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

    ItemService.prototype.update = function (paramsObject, cb) {
        var idObject = angular.extend( paramsObject, {id:this.id});
        return ItemService.update(idObject, this, cb);
    };


    ItemService.prototype.destroy = function (cb) {
        return ItemService.remove({id:this.id}, cb);
    };

    return ItemService;
}]
);

