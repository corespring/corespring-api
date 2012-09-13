var qtiServices = angular.module('qti.services', ['ngResource']);
var qtiDirectives = angular.module('qti.directives', ['qti.services']).config(['$locationProvider', function($locationProvider) {
    $locationProvider.html5Mode(true);
}]);
var app = angular.module('qti', ['qti.directives','qti.services']);


// base directive include for all QTI items
qtiDirectives.directive('assessmentitem', function(AssessmentSessionService, SessionDataService, $location) {
    return {
        restrict: 'E',
        controller: function($scope, $element, $attrs) {

            // get some attribute parameters
            var itemId = $attrs.csItemid; // cd:itemId
            var feedbackEnabled = $attrs.csFeedbackenabled; // cs:FeedbackEnabled
            if (feedbackEnabled == undefined) feedbackEnabled = false;

            // let query string override feedback property
            // TODO - perhaps only enable this in 'development' mode (which would be an attr param)
            var feedbackParam = $location.search().enableFeedback;
            if (feedbackParam && (feedbackParam == 'true' || feedbackParam == 'false' )) {
                feedbackEnabled = feedbackParam;
            }
            var scope = $scope;

            scope.status = 'ACTIVE';
            // init item session
            scope.itemSession = {};
            scope.itemSession.start = new Date().getTime(); // millis since epoch (maybe this should be set in an onload event?)



            scope.responses = {};

            // sets a response for a given question/interaction
            this.setResponse = function(key, responseValue) {
                scope.responses[key] = {value:responseValue};
            };
            // this is the function that submits the user responses and gets the outcomes
            this.submitResponses = function() {
                scope.itemSession.responses = scope.responses;
                scope.itemSession.finish = new Date().getTime();
                scope.itemSession = AssessmentSessionService.create(scope.itemSession);
                scope.sessionData = SessionDataService.get({id: scope.itemSession.id});
                scope.status = 'SUBMITTED';
                scope.formDisabled = true;
            };

            scope.isFeedbackEnabled = function () {
                return (feedbackEnabled && feedbackEnabled == 'true');
            }

        }
    };
});

qtiDirectives.directive('itembody', function() {
    return {
        restrict: 'E',
        transclude: true,
        template: '<span ng-transclude="true"></span><input type="submit" value="submit" ng-disabled="formDisabled" ng-click="onClick()"></input>',
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

            var feedbackExpr = 'scope.itemSession.feedBackContent[' + csFeedbackId + ']' ;
            scope.$watch('status', function(newValue, oldValue) {
                if (scope.isFeedbackEnabled() == false) return; // break if feedback is disabled
                if (newValue == 'SUBMITTED') {
                    var feedback = scope.sessionData.feedbackContents[csFeedbackId];
                    var outcomeIdentifier = attrs["outcomeidentifier"];
                    var choiceValue = attrs["identifier"];
                    var responseExpr = 'scope.itemSession.' + outcomeIdentifier;
                    var response = {};
                    try {
                        response = eval(responseExpr);
                    } catch(e) {
                        // response not found, leaving it empty
                        console.log(e.message);
                    }
                    // if the response is an array, check if the array contains the current choice
                    if (QtiUtils.compare(choiceValue, response)) {
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




