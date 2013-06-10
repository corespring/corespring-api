angular.module("tagger.services").
    factory('Organization', ['$resource', 'ServiceLookup', function($resource,ServiceLookup){
        return $resource(ServiceLookup.getUrlFor('organizations')+'/:path', {}, {
            isRoot: {method: 'GET', params: {path: 'isroot'}, isArray: false}
        });
    }])