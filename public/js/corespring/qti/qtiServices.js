'use strict';
/*
 * resource for returning inline feedback on a given choice id in QTI document
 */
qtiServices
    .factory('InlineFeedback', ['$resource', function ($resource) {
    return $resource (
        '/testplayer/item/:itemId/feedback' + '/:responseIdentifier/:identifier',
        {}
        ,
        {'get':  {method:'GET', isArray: false} }
    );

}]
);
