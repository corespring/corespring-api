var qtiServices = angular.module('qti.services', ['ngResource']);
var qtiDirectives = angular.module('qti.directives', ['qti.services']);
var app = angular.module('qti', ['qti.directives', 'qti.services']);


function QtiAppController($scope, $timeout) {

    $timeout(function () {
        if (typeof(MathJax) != "undefined") {
            MathJax.Hub.Queue(["Typeset", MathJax.Hub]);
        }
    }, 200);
}

QtiAppController.$inject = ['$scope', '$timeout'];

// base directive include for all QTI items
qtiDirectives.directive('assessmentitem', function (AssessmentSessionService) {
    return {
        restrict:'E',
        controller:function ($scope, $element, $attrs) {

            var scope = $scope;

            $scope.printMode = ( $attrs['printMode'] == "true" || false );
            // get some attribute parameters
            var itemId = $attrs.csItemid; // cs:itemId
            var itemSessionId = $attrs.csItemsessionid; // cs:itemId

            var apiCallParams = {
                itemId:itemId,
                //sessionId: "502d0f823004deb7f4f53be7",
                sessionId:itemSessionId,
                access_token:"34dj45a769j4e1c0h4wb"
            };

            // get item session - parameters for session behavior will be defined there
            // TODO it is an error if there is no session found
            scope.itemSession = AssessmentSessionService.get(apiCallParams);

            //TODO: Setting this to true by default
            var feedbackEnabled = (scope.itemSession.feedbackEnabled || true);

            scope.status = 'ACTIVE';

            scope.itemSession.start = new Date().getTime(); // millis since epoch (maybe this should be set in an onload event?)

            scope.responses = [];

            // sets a response for a given question/interaction
            this.setResponse = function (key, responseValue) {

                var itemResponse = this.findItemByKey(key);
                //if its null - create it

                if (!itemResponse) {
                    itemResponse = (itemResponse || { id:key });
                    scope.responses.push(itemResponse);
                }

                itemResponse.value = responseValue;
            };

            this.findItemByKey = function (key) {
                for (var i = 0; i < scope.responses.length; i++) {
                    if (scope.responses[i] && scope.responses[i].id == key) {
                        return scope.responses[i];
                    }
                }


                return null;
            };

            // this is the function that submits the user responses and gets the outcomes
            this.submitResponses = function () {
                scope.itemSession.responses = scope.responses;
                scope.itemSession.finish = new Date().getTime();
                AssessmentSessionService.save(apiCallParams, scope.itemSession, function (data) {
                    console.log("saved");
                    console.log(data);
                    scope.itemSession = data;
                    scope.status = 'SUBMITTED';
                });
                scope.formDisabled = true;
            };

            scope.isFeedbackEnabled = function () {
                return feedbackEnabled;
            }

        }
    };
});

qtiDirectives.directive('itembody', function () {

    var dots = function(count) {
       var out = "";
        for( var i = 0; i < count ; i++){
           out += "&nbsp;&nbsp;.";
        }
        return out;
    };

    return {
        restrict:'E',
        transclude:true,
        template: [
            '<span ng-show="printMode"><p>Name: ' + dots(50) + '</p></span>',
            '<span ng-transclude="true"></span>',
            '<input ng-show="!printMode" type="submit" value="submit" class="submit" ng-disabled="formDisabled" ng-click="onClick()"></input>'
        ].join('\n'),
        //replace: true,
        require:'^assessmentitem',
        link:function (scope, element, attrs, AssessmentItemCtrl) {

            scope.onClick = function () {
                AssessmentItemCtrl.submitResponses()
            };

        }

    }
});






