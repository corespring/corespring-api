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
            var noResponseAllowed = true;

            $scope.printMode = ( $attrs['printMode'] == "true" || false );

            $scope.$on('reset', function (event) {
                $scope.$broadcast('resetUI');
                $scope.formDisabled = false;
            });

            $scope.$watch('itemSession', function (newValue) {

                if (!newValue) {
                    return;
                }
                itemId = newValue.itemId;
                sessionId = newValue.id;

                if (newValue.settings) {
                    noResponseAllowed = newValue.settings.allowEmptyResponses;
                }
                $scope.$broadcast('resetUI');
                $scope.formDisabled = false;
            });

            $scope.formDisabled = true;
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


            var areResponsesIncorrect = function(){

            };

            // this is the function that submits the user responses and gets the outcomes
            this.submitResponses = function () {
                if ($scope.formDisabled) return;


                if ($scope.hasEmptyResponse() && !noResponseAllowed) {
                    $scope.status = 'ATTEMPTED';
                    $scope.showNoResponseFeedback = ($scope.hasEmptyResponse());
                    return;
                }

                $scope.$broadcast('resetUI');

                $scope.itemSession.responses = $scope.responses;

                AssessmentSessionService.save({itemId:itemId, sessionId:sessionId}, $scope.itemSession, function (data) {

                    $scope.itemSession = data;

                    $scope.showResponsesIncorrect = areResponsesIncorrect();

                    /**
                     * Note: need to call this within a $timeout
                     * as the propogation isn't working properly without it.
                     */
                    $timeout(function () {
                        $scope.formDisabled = $scope.itemSession.isFinished;
                        $scope.$broadcast('onFormDisabled', $scope.formDisabled);
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

            $scope.isFeedbackEnabled = function () {
                return isSettingEnabled("showFeedback");
            };

            $scope.highlightCorrectResponse = function () {
                return isSettingEnabled("highlightCorrectResponse")
            };

            $scope.highlightUserResponse = function () {
                return isSettingEnabled("highlightUserResponse")
            };
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
            '<a ng-show="!printMode" class="btn btn-primary" ng-disabled="formDisabled || !canSubmit" ng-click="onClick()">Submit</a>',
        ].join('\n'),
        require:'^assessmentitem',
        link:function (scope, element, attrs, AssessmentItemCtrl) {
            scope.onClick = function () {
                AssessmentItemCtrl.submitResponses()
            };
        }
    }
});






