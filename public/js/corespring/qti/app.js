var qtiServices = angular.module('qti.services', ['ngResource']);
var qtiDirectives = angular.module('qti.directives', ['qti.services']);
var app = angular.module('qti', ['qti.directives', 'qti.services']);


function QtiAppController($scope, $timeout, AssessmentSessionService) {

    $timeout(function () {
        if (typeof(MathJax) != "undefined") {
            MathJax.Hub.Queue(["Typeset", MathJax.Hub]);
        }
    }, 200);

    $scope.reset = function () {
        $scope.$broadcast('reset');
    };

    $scope.reloadItem = function () {
        console.log("reloadItem..");

        var apiCallParams = {
            itemId:$scope.itemSession.itemId,
            sessionId:$scope.itemSession.id
        };

        AssessmentSessionService.update2(apiCallParams, $scope.itemSession, function (data) {
            $scope.itemSession = data;
        });

    }
}

QtiAppController.$inject = ['$scope', '$timeout', 'AssessmentSessionService'];


function ControlBarController($scope) {

    $scope.showAdminOptions = false;
}

ControlBarController.$inject = ['$scope'];

// base directive include for all QTI items
qtiDirectives.directive('assessmentitem', function (AssessmentSessionService, $http) {
    return {
        restrict:'E',
        controller:function ($scope, $element, $attrs, $timeout) {

            $scope.printMode = ( $attrs['printMode'] == "true" || false );
            var noResponseAllowed = $attrs.csNoresponseallowed;

            var apiCallParams = {
                itemId:$attrs.csItemid,
                sessionId:$attrs.csItemsessionid,
                access_token:"34dj45a769j4e1c0h4wb"
            };

            var createNewItemSession = function () {

                var createUrl = AssessmentSessionService.getCreateUrl(apiCallParams.itemId, apiCallParams.access_token);
                $http({method:'POST', url:createUrl }).
                    success(function (data, status, headers, config) {
                        apiCallParams.sessionId = data.id;
                    }).
                    error(function (data, status, headers, config) {
                        throw "Error creating new ItemSession";
                    });
            };


            $scope.$watch('itemSession', function(newValue){
                console.log("assessment item - new itemSession: " + newValue)
            });

            $scope.$on('reset', function (event) {
                createNewItemSession();
                $scope.$broadcast('resetUI');
                $scope.formDisabled = false;
            });


            // get item session - parameters for session behavior will be defined there
            // TODO it is an error if there is no session found
            $scope.itemSession = AssessmentSessionService.get(apiCallParams);

            $scope.feedbackEnabled = ($scope.itemSession.feedbackEnabled || true);
            $scope.tryAgainEnabled = ($scope.itemSession.tryAgainEnabled || true);

            $scope.showNoResponseFeedback = false;
            $scope.itemSession.start = new Date().getTime(); // millis since epoch (maybe this should be set in an onload event?)
            $scope.responses = [];


            $scope.isEmptyItem = function (value) {
                if (!value || value == undefined) {
                    return true;
                }
                else if (typeof value == "string" && value == "") {
                    return true;
                }
                else if (Object.prototype.toString.call(value) == "[object Array]" && value.length == 0) {
                    return true;
                }
                return false;
            }

            $scope.hasEmptyResponse = function () {
                for (var i = 0; i < $scope.responses.length; i++) {
                    if ($scope.isEmptyItem($scope.responses[i].value)) return true;
                }
                return false;
            }

            // sets a response for a given question/interaction
            this.setResponse = function (key, responseValue) {

                var itemResponse = this.findItemByKey(key);
                //if its null - create it

                if (!itemResponse) {
                    itemResponse = (itemResponse || { id:key });
                    $scope.responses.push(itemResponse);
                }

                itemResponse.value = responseValue;
                $scope.canSubmit = noResponseAllowed || !$scope.hasEmptyResponse();
                $scope.showNoResponseFeedback = ($scope.status == 'ATTEMPTED' && $scope.hasEmptyResponse());
            };

            this.findItemByKey = function (key) {
                for (var i = 0; i < $scope.responses.length; i++) {
                    if ($scope.responses[i] && $scope.responses[i].id == key) {
                        return $scope.responses[i];
                    }
                }
                return null;
            };

            // this is the function that submits the user responses and gets the outcomes
            this.submitResponses = function () {
                if ($scope.formDisabled) return;


                if ($scope.hasEmptyResponse()) {
                    $scope.status = 'ATTEMPTED';
                    $scope.showNoResponseFeedback = ($scope.hasEmptyResponse());
                    return;
                }

                $scope.$broadcast('resetUI');

                $scope.itemSession.responses = $scope.responses;

                if ($scope.tryAgainEnabled) {
                    delete $scope.itemSession.finish;
                } else {
                    $scope.itemSession.finish = new Date().getTime();
                }

                AssessmentSessionService.process(apiCallParams, $scope.itemSession, function (data) {

                    $scope.itemSession = data;

                    if (!$scope.tryAgainEnabled) {
                        $scope.formDisabled = true;
                    } else {
                        $scope.status = '';
                    }
                }, function onError(error) {
                    if (error && error.data) alert(error.data.message);
                });

            };

            $scope.isFeedbackEnabled = function () {
                return $scope.feedbackEnabled;
            }
        }
    };
});

qtiDirectives.directive('itembody', function () {

    return {
        restrict:'E',
        transclude:true,
        template:[
            '<div ng-show="printMode" class="item-body-dotted-line">Name: </div>',
            '<span ng-transclude="true"></span>',
            '<div class="noResponseFeedback" ng-show="showNoResponseFeedback">Some information seems to be missing. Please provide an answer and then click "Submit". </div>',
            '<a ng-show="!printMode" class="btn btn-primary" ng-disabled="formDisabled || !canSubmit" ng-click="onClick()">Submit</a>'
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






