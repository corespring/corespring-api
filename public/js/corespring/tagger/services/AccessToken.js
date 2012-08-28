'use strict';

servicesModule
    .factory('AccessToken', [ '$resource', 'ServiceLookup', function ($resource, ServiceLookup) {

    var AccessToken = {};

    var resource = $resource( ServiceLookup.getUrlFor('getAccessToken'))

    resource.get(function(result){
        AccessToken.token = result.access_token;
        console.log("Access token received: " + AccessToken.token);
    });

    return AccessToken;
}]
);


