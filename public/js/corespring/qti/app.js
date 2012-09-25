var qtiServices = angular.module('qti.services', ['ngResource']);
var qtiDirectives = angular.module('qti.directives', ['qti.services']);
var app = angular.module('qti', ['qti.directives','qti.services']);


// base directive include for all QTI items
qtiDirectives.directive('assessmentitem', function(AssessmentSessionService) {
    return {
        restrict: 'E',
        controller: function($scope, $element, $attrs) {

            var scope = $scope;

            // get some attribute parameters
            var itemId = $attrs.csItemid; // cs:itemId
            var itemSessionId = $attrs.csItemsessionid; // cs:itemId

            var apiCallParams = {
                itemId: itemId,
                sessionId: itemSessionId,
                access_token: "34dj45a769j4e1c0h4wb"
            };

            // get item session - parameters for session behavior will be defined there
            // TODO it is an error if there is no session found
            scope.itemSession = AssessmentSessionService.get( apiCallParams );

            var feedbackEnabled = scope.itemSession.feedbackEnabled;
            if (feedbackEnabled == undefined) feedbackEnabled = false;

            scope.status = 'ACTIVE';

            scope.itemSession.start = new Date().getTime(); // millis since epoch (maybe this should be set in an onload event?)

            scope.responses = [];

            // sets a response for a given question/interaction
            this.setResponse = function(key, responseValue) {

                var itemResponse = this.findItemByKey(key);
                //if its null - create it

                if(!itemResponse) {
                    itemResponse = (itemResponse || { id : key });
                    scope.responses.push(itemResponse);
                }

                itemResponse.value = responseValue;
            };

            this.findItemByKey = function(key){
                for(var i = 0; i < scope.responses.length ; i++){
                    if( scope.responses[i] && scope.responses[i].id == key ){
                        return scope.responses[i];
                    }
                }


                return null;
            };

            // this is the function that submits the user responses and gets the outcomes
            this.submitResponses = function() {
                scope.itemSession.responses = scope.responses;
                scope.itemSession.finish = new Date().getTime();
                scope.itemSession = AssessmentSessionService.save( apiCallParams, scope.itemSession);
                scope.status = 'SUBMITTED';
                scope.formDisabled = true;
            };

            scope.isFeedbackEnabled = function () {
                return feedbackEnabled;
            }

        }
    };
});

qtiDirectives.directive('itembody', function() {
    return {
        restrict: 'E',
        transclude: true,
        template: '<span ng-transclude="true"></span><input type="submit" value="submit" class="submit" ng-disabled="formDisabled" ng-click="onClick()"></input>',
        //replace: true,
        require: '^assessmentitem',
        link: function(scope, element, attrs, AssessmentItemCtrl) {

            scope.onClick = function() {
                AssessmentItemCtrl.submitResponses()
            };

        }

    }
});


/**
 * Shared function for handling feedback blocks
 * @return {Object}
 */
var feedbackDirectiveFunction = function (QtiUtils) {

    return {
        restrict: 'E',
        template: '<span class="{{cssClass}}" ng-bind-html-unsafe="feedback"></span>',
        scope: true,
        require: '^assessmentitem',
        link: function(scope, element, attrs, AssessmentItemCtrl, $timeout) {
            scope.cssClass = element[0].localName;
            var csFeedbackId = attrs["csfeedbackid"];

            scope.$watch('status', function(newValue, oldValue) {
                if (scope.isFeedbackEnabled() == false) return; // break if feedback is disabled
                if (newValue == 'SUBMITTED') {
                    var feedback = scope.itemSession.sessionData.feedbackContents[csFeedbackId];
                    var responseIdentifier = attrs["responseidentifier"];
                    var correctResponse = scope.itemSession.sessionData.correctResponse[responseIdentifier];
                    var choiceValue = attrs["identifier"];
                    var responseExpr = 'scope.itemSession.responses.' + responseIdentifier +".value";
                    var response = {};
                    try {
                        response = eval(responseExpr);
                    } catch(e) {
                        // response not found, leaving it empty
                        console.log(e.message);
                    }
                    // if the response is an array, check if the array contains the current choice
                    if (QtiUtils.compare(choiceValue, correctResponse) || QtiUtils.compare(choiceValue, response)) {
                        scope.feedback = feedback;
                    }
                }
            });
            scope.feedback = "";
        },
        controller: function($scope) {
            this.scope = $scope;
        }
    }
};

qtiDirectives.directive('feedbackinline', feedbackDirectiveFunction);

qtiDirectives.directive('modalfeedback', feedbackDirectiveFunction);




