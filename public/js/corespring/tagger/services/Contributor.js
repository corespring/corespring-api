angular.module('tagger.services')
    .factory('Contributor',
    [ '$resource', 'ServiceLookup', function ($resource, ServiceLookup) {

        return $resource(
            ServiceLookup.getUrlFor('contributor'),
            {},
            {
                get: {method: 'GET', isArray: true}
            }
        );
    }]);
