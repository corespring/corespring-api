'use strict';
/*
 * resource for returning inline feedback on a given choice id in QTI document
 */
qtiServices
    .factory('InlineFeedback', ['$resource', function ($resource) {
    return $resource(
        '/testplayer/item/:itemId/feedback' + '/:responseIdentifier/:identifier',
        {},
        {   get:{method:'GET', isArray:false},
            save:{method:'PUT'}
        }
    );

}]
);

qtiServices.factory('AssessmentSessionService', ['$resource', function ($resource) {

    var mockData = {
      sessionData: {
          feedbackContents: {
                //120: "Correct"
                //128: "Incorrect"
//              winterDiscontent: "Wrong again!"
          }
      }
    };

    var baseUrl = '/api/v1/items/:itemId/sessions';

    var AssessmentSessionService = $resource(
        baseUrl + '/:sessionId',
        {},
        {
            get:{method:'GET', isArray:false},
            save:{method:'PUT'},
            create: {method: 'POST', params : {} }
        }
    );

    AssessmentSessionService.getCreateUrl = function( itemId, accessToken ){
        return baseUrl.replace(":itemId", itemId) + "?access_token=" + accessToken;
    };

    return AssessmentSessionService;
}]);

qtiServices
    .factory('QtiUtils', function () {
        var QtiUtils = {};

        QtiUtils.ERROR = {
            unefinedElements: "Error: can't compare 2 undefined elements"
        };

        // TODO - this will need to support other comparisons... e.g. two arrays for orderInteraction to ensure correct order & other QTI response types like matching?
        // function checks if value == response, or if response is array it checks if the array contains the value
        QtiUtils.compare = function (choiceValue, response) {

            if (choiceValue === undefined && response === undefined) {
                throw QtiUtils.ERROR.unefinedElements;
            }

            if (response instanceof Array) {
                if (response.indexOf(choiceValue) != -1) {
                    return true;
                } else {
                    return false;
                }
            }
            if (response == choiceValue) {
                return  true;
            } else {
                return false;
            }
        };

        QtiUtils.getResponseById = function (id, responses) {

            if (!id || !responses) {
                return null;
            }

            for (var i = 0; i < responses.length; i++) {
                if (responses[i].id == id) {
                    return responses[i];
                }
            }

            return null;
        };


        /**
         * Get the value from the response object
         * @param id
         * @param responses
         * @param defaultValue
         * @return {*}
         */
        QtiUtils.getResponseValue = function (id, responses, defaultValue ) {
            defaultValue = (defaultValue || "");

            try {
                var response = QtiUtils.getResponseById(id, responses);
                if (response)  return response.value;
            } catch (e) {
                // just means it isn't set, leave it as ""
            }
            return defaultValue;
        };


        return QtiUtils;
    }
);
