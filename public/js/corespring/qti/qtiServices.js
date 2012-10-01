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

//The session service
qtiServices.factory('AssessmentSessionService', ['$resource', function ($resource) {

    var mockData = {
      sessionData: {
          feedbackContents: {
              winterDiscontent: "Wrong again!"
          }
      }
    };

    var AssessmentSessionService = $resource(
        '/api/v1/items/:itemId/sessions/:sessionId',
        {},
        { get:{method:'GET', isArray:false},
            save:{method:'PUT'}
        }
    );

    //stash the default update function from angular
    var ngSave = AssessmentSessionService.save;

    AssessmentSessionService.save = function() {

        var getCallbackIndex = function(argArray){
            if(!argArray){ return -1; }
            for(var i = 0; i < argArray.length ; i++){
                if( typeof(argArray[i]) == "function"){
                    return i;
                }
            }
            return -1;
        };


        var callbackIndex = getCallbackIndex(arguments);

        if( callbackIndex == -1 ){
            ngSave.apply( AssessmentSessionService, arguments );
        } else {

            var callback = arguments[callbackIndex];


            /**
             * A callback wrapper - so we can mock some responses whilst we are developing.
             * @param data
             */
            var callbackWrapper = function(data){

                if( !data.sessionData || !data.sessionData.feedbackContents ){
                    callback(data);
                } else {
                     //angular.extend(data.sessionData, mockData.sessionData);

                    for( var x in mockData.sessionData.feedbackContents ){
                        if(!data.sessionData.feedbackContents[x]){
                            data.sessionData.feedbackContents[x] = mockData.sessionData.feedbackContents[x];
                        }
                    }
                    callback(data);
                }
            };

            var newArguments = [];
            for( var i = 0; i < arguments.length ; i++ ){
                newArguments.push( callbackIndex == i ? callbackWrapper : arguments[i]);
            }

            ngSave.apply(AssessmentSessionService, newArguments);
        }
    };

    return AssessmentSessionService;
}]);

qtiServices
    .factory('QtiUtils', function () {
        var QtiUtils = {};
        // TODO - this will need to support other comparisons... e.g. two arrays for orderInteraction to ensure correct order & other QTI response types like matching?
        // function checks if value == response, or if response is array it checks if the array contains the value
        QtiUtils.compare = function (choiceValue, response) {

            if (choiceValue === undefined && response === undefined) {
                throw "Error: can't compare 2 undefined elements";
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

        // shared service for re-arranging simple-choice elements according to
        // shuffle and fixed attributes. Should be called from compile functions for interactions that support these props
        QtiUtils.shuffleChoices = function (element) {
            // TODO - implement me, and call this from compile function in choiceInteraction, orderInteraction print/web
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
        };


        return QtiUtils;
    }
);
