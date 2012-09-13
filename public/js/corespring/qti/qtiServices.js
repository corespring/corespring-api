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

/*
 * Mock Resource Service
 */
qtiServices
    .factory('AssessmentSession', ['$resource', function ($resource) {
    var AssessmentSession = {};
    AssessmentSession.create = function(session) {
        // i'm guessing that outcome variables would be declared in base scope like this...
        session.score = 1;
        // putting an outcome property for each response, but maybe they should just be
        // at response.score instead of response.outcome.score ?
        for(var i=0, len=session.responses.length; i < len; i++){
            var response = session.responses[i];
            response.outcome = {score: 1};
        }

        // the server could attach feedbackContents to the response to let the client show it as necessary...
        // of course these would be random guids in real life
        session.sessionData = {};
        session.sessionData.feedbackContents = {};
        session.sessionData.feedbackContents.bush = "George Bush was president of the USA";
        session.sessionData.feedbackContents.blair = "Tony Blair was president of the UK";
        session.sessionData.feedbackContents.sharon = "Ariel Sharon was the president of the Israel";
        session.sessionData.feedbackContents.calderon = "Correct, <b>Felipe Calderon</b> is the president of Mexico";
        session.sessionData.feedbackContents.robinson = "Mary Robinson was president of Ireland in the nineties";
        session.sessionData.feedbackContents.higgins = "Correct! Michael D Higgins is the current president of Ireland";
        session.sessionData.feedbackContents.adams = "Gerry Adams is the leader of the Sinn Fein political party";
        session.sessionData.feedbackContents.guinness = "Arthur Guinness is the founder of Guinness Brewery";

        session.sessionData.feedbackContents.blue = "Correct, blue is a color in the rainbow";
        session.sessionData.feedbackContents.violet = "Correct, violet is a color in the rainbow";
        session.sessionData.feedbackContents.white = "<b>Incorrect</b>, white is not a color in the rainbow";
        session.sessionData.feedbackContents.red = "Correct, red is a color in the rainbow";

        session.sessionData.feedbackContents.fbWinter = "York is correct";

        // sessionData correct responses
        session.sessionData.correctResponse = {};
        session.sessionData.correctResponse.mexicanPresident = 'calderon';
        session.sessionData.correctResponse.irishPresident = 'higgins';
        session.sessionData.correctResponse.rainbowColors = ['red','violet','blue'];
        session.sessionData.correctResponse.wivesOfHenry = ['aragon','boleyn','seymour','cleves','howard','parr'];
        session.sessionData.correctResponse.cutePugs = ['pug3','pug1','pug2'];

        // should send this back in the sessionData, but won't work for inline feedback, could work in modalFeedback...
        session.sessionData.correctResponse.winterDiscontent = 'York';

        return session;



    };
    return AssessmentSession;

}]
);

qtiServices
    .factory('QtiUtils', ['$resource', function ($resource) {
    var QtiUtils = {};

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
    return QtiUtils;
}
]);
