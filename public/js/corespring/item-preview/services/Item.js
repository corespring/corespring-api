try{
    angular.module('preview.services')
}
catch(e) {
    angular.module('preview.services', ['ngResource', 'tagger.services']);
}

angular.module('preview.services')
.factory('Item',
    [ '$resource', 'ServiceLookup',
        function ($resource, ServiceLookup) {
            return $resource(
                ServiceLookup.getUrlFor('items'),
                {},
                {
                    query:{ method:'GET', isArray:true},
                    count:{ method:'GET', isArray:false}
                }
            );
        }
    ]);
