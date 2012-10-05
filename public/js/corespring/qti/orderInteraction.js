/**
 * Interaction for ordering a set of choices
 */

qtiDirectives.directive('orderinteraction', function (QtiUtils) {


    var choiceTemplate = [
        '<span ng-bind-html-unsafe="prompt" class="choice-prompt"></span>',
        '<div sortable="" class="sortable-body" ng-class="{noResponse: noResponse}">',
        '<div ng:repeat="item in items" obj="{{item}}"> ',
        '<span class="sortable-item {{item.submittedClass}}" ng-bind-html-unsafe="item.content"></span>',
        ' </div>',
        '</div>'].join('');


    return {
        restrict:'E',
        scope:true,
        require:'^assessmentitem',
        compile:function (tElement, tAttrs, transclude) {
            // compile function

            // get the prompt element if present
            // it supports embedded html
            var result = tElement.find("prompt");
            var prompt = "";
            if (result.length == 1) {
                var promptElem = angular.element(result[0]);
                prompt = promptElem.html();
            }

            // get the simple-choice elements
            // they support embedded html
            var choices = [];
            var fixedIndexes = [];
            var choiceElements = angular.element(tElement).find("simpleChoice");
            for (var i = 0; i < choiceElements.length; i++) {
                var elem = angular.element(choiceElements[i]);
                var identifier = elem.attr('identifier');
                var fixed = elem.attr('fixed') == "true";

                if(fixed){
                    fixedIndexes.push(i);
                }

                choices.push({content:elem.html(), identifier:identifier});
            }

            choices.shuffle(fixedIndexes);

            // now modify the DOM
            tElement.html(choiceTemplate);


            // linking function
            return function ($scope, element, attrs, AssessmentItemCtrl) {


                var responseIdentifier = attrs["responseidentifier"];
                // set model to choices extracted from html
                $scope.orderedList = [];
                $scope.result = [];
                $scope.prompt = prompt;
                $scope.items = choices;
                $scope.changed = false;


                var updateAssessmentItem = function (orderedList) {
                    var flattenedArray = [];
                    for (var i = 0; i < orderedList.length; i++) {
                        flattenedArray[i] = orderedList[i].identifier;
                    }
                    AssessmentItemCtrl.setResponse(responseIdentifier, flattenedArray);
                    $scope.changed = true;
                };

                $scope.$watch('showNoResponseFeedback', function(newVal, oldVal) {
                    $scope.noResponse = (!$scope.changed && $scope.showNoResponseFeedback);
                });

                // watch the response and set it to the responses list
                $scope.$watch('orderedList', function (newValue, oldValue) {
                    if (oldValue.length == 0 || newValue.length == 0) {
                        AssessmentItemCtrl.setResponse(responseIdentifier, []);
                    } else {
                        updateAssessmentItem(newValue);
                    }
                    $scope.noResponse = (!$scope.changed && $scope.showNoResponseFeedback);
                });

                var setAllIncorrect = function(){ applyCssNameToAll("order-incorrect"); };

                var applyCssNameToAll = function( name ){
                    for (var y = 0; y < $scope.items.length; y++) {
                        $scope.items[y].submittedClass = name;
                    }
                };

                var applyCss = function(correctResponse, ourResponse) {

                    setAllIncorrect();

                    for (var i = 0; i < correctResponse.length; i++) {
                        if (correctResponse[i] == ourResponse[i]) {
                            for (var x = 0; x < $scope.items.length; x++) {
                                if ($scope.items[x].identifier == ourResponse[i]) {
                                    $scope.items[x].submittedClass = "order-correct";
                                }
                            }
                        }
                    }
                };

                /**
                 * Reset the ui.
                 */
                $scope.$on('resetUI', function (event) {
                    applyCssNameToAll("");
                });


                $scope.$watch('itemSession.sessionData.correctResponses', function (responses) {
                    if(!responses) return;
                    if(!$scope.isFeedbackEnabled()) return;
                    var correctResponse = responses[responseIdentifier];
                    var ourResponse = QtiUtils.getResponseValue(responseIdentifier, $scope.itemSession.responses, [])
                    applyCss(correctResponse, ourResponse)
                });

            };
        }

    }
});


qtiDirectives.directive("sortable", function () {
    return {
        // todo look into isolate scope so orderedList is not on global scope, tried it but was having trouble
        link:function (scope, el, attrs, ctrl, $timeout) {

            var startParent = null;
            var stopParent = null;

            var buildItemsList = function ($node) {
                var items = [];
                $node.children('div').each(function (index) {
                    var liItem = scope.$eval($(this).attr('obj'));
                    liItem.ord = index;
                    items.push(liItem);
                });

                return items;
            };

            scope.$watch("formDisabled", function( newValue ){
               $(el).sortable( "option", "disabled", newValue === true );
            });


            $(el).sortable({
                items:'div:not(:has(div.complete))',
                start:function (event, ui) {
                    startParent = $(ui.item).parent();
                },

                /**
                 * Adding a create event handler so that we can init the list correctly.
                 * @param event
                 * @param ui
                 */
                create:function (event, ui) {

                    var target = event.target;

                    setTimeout(
                        function () {
                            var items = buildItemsList($(target));
                            scope.$apply(function () {
                                scope.orderedList = items;
                            });
                        }, 500
                    );
                },
                stop:function (event, ui) {
                    stopParent = $(ui.item).parent();
                    var items = buildItemsList(stopParent);
                    scope.$apply(function () {
                        scope.orderedList = items;
                    });

                },
                connectWith:'.sortable-linked',
                placeholder:'ui-state-highlight',
                forcePlaceholderSize:true,
                axis:'y',
                dropOnEmpty:true
            });
        }
    }
});