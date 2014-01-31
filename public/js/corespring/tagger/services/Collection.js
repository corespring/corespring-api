angular.module('tagger.services')
    .factory('Collection', [ '$resource', 'ServiceLookup', function ($resource, ServiceLookup) {
    var Collection = $resource(
        ServiceLookup.getUrlFor('collection') + '/:id',
        { },
        {
            create: {method:'POST'},
            update:{ method:'PUT' },
            count:{method:'GET', isArray:false},
            get: {method: 'GET', isArray: true}
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

angular.module('tagger.services')
  .factory('CollectionEnabledStatus', [
    '$resource', 'ServiceLookup', function($resource, ServiceLookup){

      return $resource(
        ServiceLookup.getUrlFor('collectionSetEnabledStatus'),
        {},
        {
          "update": { method : 'PUT'}
        }
      );
    }
  ]);