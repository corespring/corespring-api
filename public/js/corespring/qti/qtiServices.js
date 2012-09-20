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

/**
 *
 * Mock Resource Service
 * When the item player loads it will be given an itemSessionId of a session that was already
 * created, it will load those properties by calling the get() method
 */
qtiServices
    .factory('AssessmentSessionService', ['$resource', function ($resource) {
    var AssessmentSessionService = {};

    // create a mock session
    var mockSession = {};
    mockSession.start = new Date().getTime();
    mockSession.feedbackEnabled = true;
    mockSession.role = 'candidate';
    mockSession.multipleAttemptsAllowed = true;

    AssessmentSessionService.get = function(obj) {
        return mockSession;
    };

    AssessmentSessionService.update = function(session) {
        // i'm guessing that outcome variables would be declared in base scope like this...
        session.score = 1;

        for(var i=0, len=session.responses.length; i < len; i++){
            var response = session.responses[i];
            response.outcome = {score: 1};
        }

        return session;



    };
    return AssessmentSessionService;

}]
);

/**
 * This might be implemented as part of ItemSessionAPI, but under a different route
 * like /api/v1/itemsession/:itemSessionId/sessiondata
 */
qtiServices
    .factory('SessionDataService', ['$resource', function ($resource) {
    var SessionDataService = {};

    SessionDataService.get = function(obj) {
        // when we switch to using resource obj will be {id: someId}
        // id will be the item session id
        // TODO - the server implementation would need to ensure that response was already submitted before returning this data
        // responses may not be submitted more than once unless the runtime options for the item session allow that

        // This is mock data
        // The server will need to create the feedbackContents object
        // the properties will be the csFeedbackIds that were specified in the xml (or automatically added to the xml)
        var data = {};
        data.feedbackContents = {};
        data.feedbackContents.obama = "Barack Obama is president of the USA";
        data.feedbackContents.cameron = "David Cameron is Prime Minister of the UK";
        data.feedbackContents.netanyahu = "Benjamin Netanyahu is the Prime Minister Israel";
        data.feedbackContents.calderon = "Correct, <b>Felipe Calderon</b> is the president of Mexico";

        data.feedbackContents.fbRobinson = "Mary Robinson was president of Ireland in the nineties";
        data.feedbackContents.fbHiggins = "Correct! Michael D Higgins is the current president of Ireland";
        data.feedbackContents.fbAdams = "Gerry Adams is the leader of the Sinn Fein political party";
        data.feedbackContents.fbGuinness = "Arthur Guinness is the founder of Guinness Brewery";

        data.feedbackContents.blue = "Correct, blue is a color in the rainbow";
        data.feedbackContents.violet = "Correct, violet is a color in the rainbow";
        data.feedbackContents.white = "<b>Incorrect</b>, white is not a color in the rainbow";
        data.feedbackContents.red = "Correct, red is a color in the rainbow";

        data.feedbackContents.fbWinter = "York is correct";

        // sessionData correct responses
        // server service will need to process the QTI and prepare this object with the correct responses
        // using e.b. <responseDeclaration idenfifier="mexicanPresident"
        data.correctResponse = {};
        data.correctResponse.mexicanPresident = 'calderon';
        data.correctResponse.irishPresident = 'higgins';
        data.correctResponse.rainbowColors = ['red','violet','blue'];
        data.correctResponse.wivesOfHenry = ['aragon','boleyn','seymour','cleves','howard','parr'];
        data.correctResponse.cutePugs = ['pug3','pug1','pug2'];

        // should send this back in the sessionData, but won't work for inline feedback, could work in modalFeedback...
        data.correctResponse.winterDiscontent = 'York';

        return data;

    };

    return SessionDataService;
    }
]);

qtiServices
    .factory('QtiUtils', function () {
    var QtiUtils = {};
    // TODO - this will need to support other comparisons... e.g. two arrays for orderInteraction to ensure correct order & other QTI response types like matching?
    // function checks if value == response, or if response is array it checks if the array contains the value
    QtiUtils.compare = function(choiceValue, response) {
        if (response instanceof Array) {
            if ( response.indexOf(choiceValue) != -1 ) {
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
    QtiUtils.shuffleChoices = function(element) {
        // TODO - implement me, and call this from compile function in choiceInteraction, orderInteraction print/web
    };

    return QtiUtils;
}
);
