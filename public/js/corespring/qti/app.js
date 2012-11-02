var qtiServices = angular.module('qti.services', ['ngResource']);
var qtiDirectives = angular.module('qti.directives', ['qti.services']);
var app = angular.module('qti', ['qti.directives', 'qti.services']);


function QtiAppController($scope, $timeout, $location, AssessmentSessionService) {

    $timeout(function () {
        if (typeof(MathJax) != "undefined") {
            MathJax.Hub.Queue(["Typeset", MathJax.Hub]);
        }
    }, 200);

    $scope.reset = function () {
        $scope.$broadcast('reset');
    };

    $scope.init = function () {
        var url = $location.absUrl();
        var matches = url.match(/.*\/item\/(.*?)\/.*/);
        var params = { itemId:matches[1] };
        AssessmentSessionService.create(params, {}, function (data) {
            $scope.itemSession = data;
        });
    };

    /**
     * Because the current item session has been started - its settings are now locked.
     * So we are going to be creating a new item session.
     */
    $scope.reloadItem = function () {
        AssessmentSessionService.create({itemId:$scope.itemSession.itemId}, $scope.itemSession, function (data) {
            $scope.reset();
            $scope.itemSession = data;
        });
    };

    $scope.init();
}

QtiAppController.$inject = ['$scope', '$timeout', '$location', 'AssessmentSessionService'];


function ControlBarController($scope) {

    $scope.showAdminOptions = false;
}

ControlBarController.$inject = ['$scope'];

// base directive include for all QTI items
qtiDirectives.directive('assessmentitem', function (AssessmentSessionService, $http) {
    return {
        restrict:'E',
        controller:function ($scope, $element, $attrs, $timeout) {

            var itemId = null;
            var sessionId = null;
            var allowEmptyResponses = true;

            $scope.printMode = ( $attrs['printMode'] == "true" || false );
            $scope.finalSubmit = false;

            $scope.$on('reset', function (event) {
                $scope.$broadcast('resetUI');
                $scope.formSubmitted = false;
                $scope.formHasIncorrect = false;
                $scope.finalSubmit = false;
            });

            $scope.$watch('itemSession', function (newValue) {
                if (!newValue) {
                    return;
                }
                itemId = newValue.itemId;
                sessionId = newValue.id;

                if (newValue.settings) {
                    allowEmptyResponses = newValue.settings.allowEmptyResponses;
                    $scope.canSubmit = allowEmptyResponses && $scope.hasEmptyResponse();
                }
                $scope.$broadcast('resetUI');
                $scope.formSubmitted = false;
            });

            $scope.showNoResponseFeedback = false;
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
            };

            $scope.hasEmptyResponse = function () {

                for (var i = 0; i < $scope.responses.length; i++) {
                    if ($scope.isEmptyItem($scope.responses[i].value)) return true;
                }
                return false;
            };

            // sets a response for a given question/interaction
            this.setResponse = function (key, responseValue) {

                var itemResponse = this.findItemByKey(key);

                //if its null - create it
                if (!itemResponse) {
                    itemResponse = (itemResponse || { id:key });
                    $scope.responses.push(itemResponse);
                }

                itemResponse.value = responseValue;
                $scope.canSubmit = allowEmptyResponses || !$scope.hasEmptyResponse();
                $scope.showNoResponseFeedback = ($scope.status == 'ATTEMPTED' && $scope.hasEmptyResponse());

                $scope.finalSubmit = false;
            };

            this.findItemByKey = function (key) {
                for (var i = 0; i < $scope.responses.length; i++) {
                    if ($scope.responses[i] && $scope.responses[i].id == key) {
                        return $scope.responses[i];
                    }
                }
                return null;
            };


            var areResponsesIncorrect = function(){
                if (!$scope.itemSession || !$scope.itemSession.responses) return false;
                for (var i = 0; i < $scope.itemSession.responses.length; i++) {
                    if ($scope.itemSession.responses[i].outcome != undefined && $scope.itemSession.responses[i].outcome.score < 1) return true;
                }
                return false;

            };

            // this is the function that submits the user responses and gets the outcomes
            this.submitResponses = function () {
                if ($scope.formSubmitted) return;


                if ($scope.hasEmptyResponse() && !allowEmptyResponses) {
                    $scope.status = 'ATTEMPTED';
                    $scope.showNoResponseFeedback = ($scope.hasEmptyResponse());
                    return;
                }

                $scope.$broadcast('resetUI');

                $scope.itemSession.responses = $scope.responses;

                if ($scope.finalSubmit) $scope.itemSession.finish = new Date().getTime();

                AssessmentSessionService.save({itemId:itemId, sessionId:sessionId}, $scope.itemSession, function (data) {
                    $scope.itemSession = data;
                    $scope.formHasIncorrect = areResponsesIncorrect();
                    $scope.finalSubmit = true;

                    // Note: need to call this within a $timeout as the propogation isn't working properly without it.
                    $timeout(function () {
                        $scope.formSubmitted = $scope.itemSession.isFinished;
                        if ($scope.formSubmitted) {
                            $scope.formHasIncorrect = false;
                        }
                        $scope.$broadcast('onFormDisabled', $scope.formSubmitted);
                    });

                }, function onError(error) {
                    if (error && error.data) alert(error.data.message);
                });
            };

            var isSettingEnabled = function (name) {
                if (!$scope.itemSession || !$scope.itemSession.settings) {
                    return false;
                }
                return $scope.itemSession.settings[name];
            };

            $scope.submitButtonText = function() {
                return ($scope.finalSubmit) ? "Submit Anyway" : "Submit";
            }

            $scope.isFeedbackEnabled = function () {
                return isSettingEnabled("showFeedback");
            };

            $scope.highlightCorrectResponse = function () {
                return isSettingEnabled("highlightCorrectResponse")
            };

            $scope.highlightUserResponse = function () {
                return isSettingEnabled("highlightUserResponse")
            };

            $scope.submitCompleteMessage = function () {
                if (!$scope.itemSession || !$scope.itemSession.settings) {
                    return "Your response has been received";
                }
                return $scope.itemSession.settings.submitCompleteMessage;
            }

            $scope.submitIncorrectMessage = function () {
                if (!$scope.itemSession || !$scope.itemSession.settings) {
                    return "Looks like there is something you might fix in your work. You can change your answers, or submit your response as-is";
                }
                return $scope.itemSession.settings.submitIncorrectMessage;
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
            '<div class="noResponseFeedback" ng-show="formHasIncorrect">{{submitIncorrectMessage()}}</div>',
            '<div class="noResponseFeedback" ng-show="formSubmitted">{{submitCompleteMessage()}}</div>',
            '<a ng-show="!printMode" class="btn btn-primary" ng-disabled="formSubmitted || !canSubmit" ng-hide="formSubmitted" ng-click="onSubmitClick()">{{submitButtonText()}}</a>',
        ].join('\n'),
        require:'^assessmentitem',
        link:function (scope, element, attrs, AssessmentItemCtrl) {
            scope.onSubmitClick = function () {
                AssessmentItemCtrl.submitResponses()
            };
        }
    }
});






