"use strict";

angular.module('qti.directives', ['qti.services','ngDragDrop','ui.sortable', 'corespring-logger']);
angular.module('qti', ['qti.directives', 'qti.services', 'corespring-services', 'corespring-directives','corespring-utils', 'ui', 'corespring-logger']);


function ControlBarController($scope, $rootScope) {
    $scope.showAdminOptions = false;
    $scope.showScore = false;

    $scope.toggleControlBar = function() {
        $scope.showAdminOptions = !$scope.showAdminOptions;

        $rootScope.$broadcast('controlBarChanged');
    }

    $rootScope.$on('computedOutcome', function(event, outcome){
        $scope.showScore = true;
        $scope.scorePopup = false;
        $scope.outcome = outcome;
        $scope.hasScript = outcome.script != null;
        $scope.identifiers = _.map(_.keys(outcome.identifierOutcomes),function(identifier){
            return {label: identifier, display: false};
        });
        $scope.displayIdentifier = function(label){
            _.each($scope.identifiers,function(identifier){
                if(identifier.label == label && !identifier.display) identifier.display = true;
                else identifier.display = false;
            });
        }
        $scope.seeScript = function(){
            var scriptWindow = window.open()
            scriptWindow.document.write([
            "<link href=\"http://alexgorbatchev.com/pub/sh/current/styles/shCoreDefault.css\" rel=\"stylesheet\" type=\"text/css\" />",
            "<script src=\"http://alexgorbatchev.com/pub/sh/current/scripts/shCore.js\" type=\"text/javascript\"></script>",
            "<script src=\"http://alexgorbatchev.com/pub/sh/current/scripts/shBrushJScript.js\" type=\"text/javascript\"></script>",
            "<script type=\"text/javascript\">SyntaxHighlighter.all();</script>",
            "<pre class=\"brush: js\">",
                outcome.script,
            "</pre>"
            ].join("\n"));
        }
        function createScriptElem(js){
          var e = document.createElement('script');
          e.type = 'text/javascript';
          e.src  = 'data:text/javascript;charset=utf-8,'+escape([
              js,
              "document.getElementById('scriptContent').innerHTML = JSON.stringify(outcome);"
            ].join("\n"));
          return e;
        }
        function createOutcomeElem(){
            var scriptContentElem = document.createElement("pre");
            scriptContentElem.id = "scriptContent";
            return scriptContentElem;
        }
        $scope.runScript = function(){
            var scriptResultsElem = document.getElementById("scriptResults");
            scriptResultsElem.innerHTML = "";
            var scriptElem = createScriptElem(outcome.script);
            var outcomeElem = createOutcomeElem();
            scriptResultsElem.appendChild(outcomeElem);
            scriptResultsElem.appendChild(scriptElem);
        }
    })

}

ControlBarController.$inject = ['$scope', '$rootScope'];

// base directive include for all QTI items
angular.module('qti.directives').directive('assessmentitem', ['Logger', function(Logger) {
    return {
        restrict: 'E',
        controller: function($scope, $element, $attrs, $timeout, $rootScope, $location) {

            var matchOmitButtonAttribute = /omitSubmitButton=(\w*)/.exec($location.absUrl());
            $scope.omitSubmitButton = matchOmitButtonAttribute && matchOmitButtonAttribute.length > 1 && matchOmitButtonAttribute[1] == "true";

            var itemId = null;
            var sessionId = null;
            var allowEmptyResponses = true;
            var noResponseMessage = 'Please complete your work before you submit it.';

            $scope.printMode = ($attrs['mode'] == "Printing" || false);
            $scope.finalSubmit = false;

            $scope.$on('reset', function(event) {
                $scope.$broadcast('resetUI');
                $scope.formSubmitted = false;
                $scope.formHasIncorrect = false;
                $scope.finalSubmit = false;
            });

            $scope.$watch('itemSession', function(newValue) {
                if (!newValue) {
                    return;
                }

                // We trigger MathML for possible math blocks feedbacks
                $timeout(function () {
                  if (typeof(MathJax) != "undefined") {
                    MathJax.Hub.Queue(["Typeset", MathJax.Hub]);
                  }
                }, 10);

                itemId = newValue.itemId;
                sessionId = newValue.id;

                if (newValue.settings) {
                    allowEmptyResponses = newValue.settings.allowEmptyResponses;
                    $scope.canSubmit = allowEmptyResponses && $scope.hasEmptyResponse();
                }

                $scope.formSubmitted = $scope.itemSession.isFinished;
                $scope.$broadcast('highlightUserResponses');
                if ($scope.formSubmitted) {
                    $scope.$broadcast('formSubmitted', $scope.itemSession, !areResponsesIncorrect());
                } else {
                    $scope.$broadcast('resetUI');
                }
            });

            $scope.showNoResponseFeedback = false;
            $scope.responses = [];

            $scope.isEmptyItem = function(value) {
                if (!value) {
                    return true;
                } else if (typeof(value) === "string" && value === "") {
                    return true;
                } else if (Object.prototype.toString.call(value) == "[object Array]" && value.length === 0) {
                    return true;
                }
                return false;
            };

            $scope.hasEmptyResponse = function() {

                for (var i = 0; i < $scope.responses.length; i++) {
                    if ($scope.isEmptyItem($scope.responses[i].value)) return true;
                }
                return false;
            };

            // sets a response for a given question/interaction
            this.setResponse = function(key, responseValue) {

                var itemResponse = this.findItemByKey(key);

                //if its null - create it
                if (!itemResponse) {
                    itemResponse = (itemResponse || {
                        id: key
                    });
                    $scope.responses.push(itemResponse);
                }

                itemResponse.value = responseValue;
                $scope.canSubmit = allowEmptyResponses || !$scope.hasEmptyResponse();
                $scope.showNoResponseFeedback = ($scope.status == 'ATTEMPTED' && $scope.hasEmptyResponse());

                $scope.finalSubmit = false;
            };

            this.findItemByKey = function(key) {
                for (var i = 0; i < $scope.responses.length; i++) {
                    if ($scope.responses[i] && $scope.responses[i].id == key) {
                        return $scope.responses[i];
                    }
                }
                return null;
            };


            var areResponsesIncorrect = function() {
                if (!$scope.itemSession || !$scope.itemSession.responses) return false;
                for (var i = 0; i < $scope.itemSession.responses.length; i++) {
                    if ($scope.itemSession.responses[i].outcome && $scope.itemSession.responses[i].outcome.score < 1) return true;
                }
                return false;

            };

            var that = this;
            $scope.$on("submitItem", function(event,opts) {
              if(opts && !opts.isAttempt){
                that.submitResponses(false);
              } else {
                that.submitResponses(true);
              }
            });

            // this is the function that submits the user responses and gets the outcomes
            this.submitResponses = function(isAttempt) {
                if ($scope.formSubmitted) return;

                if ($scope.hasEmptyResponse() && !allowEmptyResponses) {
                    $scope.status = 'ATTEMPTED';
                    $scope.showNoResponseFeedback = ($scope.hasEmptyResponse());
                    return;
                }

                $scope.$broadcast('resetUI');

                $scope.itemSession.responses = $scope.responses;

                if(isAttempt === false) $scope.itemSession.isAttempt = false;

                if ($scope.finalSubmit) $scope.itemSession.finish = new Date().getTime();

                var onSuccess = function() {
                    $scope.formHasIncorrect = areResponsesIncorrect();
                    $scope.finalSubmit = isAttempt;
                    // Note: need to call this within a $timeout as the propogation isn't working properly without it.
                    $timeout(function() {
                        $scope.formSubmitted = $scope.itemSession.isFinished;
                        if ($scope.formSubmitted) {
                            $scope.formHasIncorrect = false;
                            $scope.$broadcast('formSubmitted', $scope.itemSession, !areResponsesIncorrect());
                            $rootScope.$broadcast('computedOutcome', $scope.itemSession.outcome)
                        }
                    });
                };

                var onError = function(data) {
                    Logger.error("Error in assessmentItem directive when submitting responses: "+JSON.stringify(data));
                };

                $rootScope.$broadcast('assessmentItem_submit', $scope.itemSession, onSuccess, onError, !areResponsesIncorrect());
            };

            var isSettingEnabled = function(name) {
                if (!$scope.itemSession || !$scope.itemSession.settings) {
                    return false;
                }
                return $scope.itemSession.settings[name];
            };

            this.registerInteraction = function(id, prompt, type) {
                $scope.$broadcast('registerInteraction', id, prompt, type);
            };

            $scope.showFeedback = function() {
                return $scope.formHasIncorrect || $scope.formSubmitted || $scope.showNoResponseFeedback;
            };

            $scope.getFeedbackMessageClass = function() {
                if ($scope.showNoResponseFeedback) {
                    return "no-response-feedback";
                }
                if ($scope.formHasIncorrect) {
                    return "form-is-incorrect";
                }
                if ($scope.formSubmitted) {
                    return "form-submitted";
                }
                return "";
            };

            $scope.getFeedbackMessage = function() {
                if ($scope.showNoResponseFeedback) {
                    return noResponseMessage;
                }
                if ($scope.formHasIncorrect) {
                    return $scope.submitIncorrectMessage();
                }
                if ($scope.formSubmitted) {
                    return $scope.submitCompleteMessage();
                }
                return "";
            };

            $scope.isAllowedSubmit = function() {
                var out = $scope.canSubmit || !$scope.formSubmitted;
                return out;
            };

            $scope.submitButtonText = function() {
                return ($scope.finalSubmit) ? "Submit Anyway" : "Submit";
            };

            $scope.isFeedbackEnabled = function() {
                return isSettingEnabled("showFeedback");
            };

            $scope.highlightCorrectResponse = function() {
                return $scope.itemSession && $scope.itemSession.isFinished && isSettingEnabled("highlightCorrectResponse");
            };

            $scope.highlightUserResponse = function() {
                return isSettingEnabled("highlightUserResponse");
            };

            $scope.submitCompleteMessage = function() {
                if (!$scope.itemSession || !$scope.itemSession.settings) {
                    return "Ok!";
                }
                return $scope.itemSession.settings.submitCompleteMessage;
            };

            $scope.submitIncorrectMessage = function() {
                if (!$scope.itemSession || !$scope.itemSession.settings) {
                    // TODO - this is being drawn from ItemSessionSettings.scala, is this a dupe
                    return "You may revise your work before you submit it.";
                }
                return $scope.itemSession.settings.submitIncorrectMessage;
            };

            $scope.initMathML = function(delay) {
              $timeout(function() {
                if (typeof(MathJax) != "undefined") {
                  MathJax.Hub.Queue(["Typeset", MathJax.Hub]);
                }
              }, delay ? delay : 0);
            }
        }
    };
}]);

angular.module('qti.directives').directive('itembody', function() {

    return {
        restrict: 'E',
        transclude: true,
        template: [
            '<div ng-show="printMode" class="item-body-dotted-line">Name: </div>',
            '<span ng-transclude="true"></span>',
            '<div class="flowBox" ng-show="!printMode">',
            '<div class="ui-hide animatable flow-feedback-container" ng-class="{true: \'ui-show\', false: \'ui-hide\'}[showFeedback()]">',
            '<div class="feedback-message" ng-class="getFeedbackMessageClass()"><span class="text">{{getFeedbackMessage()}}</span></div>',
            '</div>',
            '<a class="btn btn-primary" ng-disabled="!isAllowedSubmit()" ng-hide="omitSubmitButton || formSubmitted" ng-click="onSubmitClick(true)">{{submitButtonText()}}</a>',
            '</div>'].join('\n'),
        require: '^assessmentitem',
        link: function(scope, element, attrs, AssessmentItemCtrl) {
            scope.onSubmitClick = function(isAttempt) {
                AssessmentItemCtrl.submitResponses(isAttempt);
            };
        }
    };
});