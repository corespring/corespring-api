/**
 * Interaction for ordering a set of choices
 */

var parseSimpleChoices = function(element) {
    // get the simple-choice elements
    // they support embedded html
    var choices = [];
    var fixedIndexes = [];
    var choiceElements = angular.element(element).find("simpleChoice");
    for (var i = 0; i < choiceElements.length; i++) {
        var elem = angular.element(choiceElements[i]);
        var identifier = elem.attr('identifier');
        var fixed = elem.attr('fixed') == "true";

        if (fixed) {
            fixedIndexes.push(i);
        }

        choices.push({content:elem.html(), identifier:identifier});
    }

    choices.shuffle(fixedIndexes);
    return choices;
}

var parsePrompt = function(element) {
    // get the prompt element if present
    // it supports embedded html
    var result = element.find("prompt");
    var prompt = "";
    if (result.length == 1) {
        var promptElem = angular.element(result[0]);
        prompt = promptElem.html();
    }
    return prompt;
}



var compileNormalOrderInteraction = function (tElement, QtiUtils) {

    var choiceTemplate = [
        '<span ng-bind-html-unsafe="prompt" class="choice-prompt"></span>',
        '<div sortable="" class="sortable-body" ng-class="{noResponse: noResponse}">',
        '<div ng:repeat="item in items" obj="{{item}}"> ',
        '<span class="sortable-item {{item.submittedClass}}" ng-bind-html-unsafe="item.content"></span>',
        ' </div>',
        '</div>'].join('');


    var localScope = {
        choices:  parseSimpleChoices(tElement),
        prompt:  parsePrompt(tElement)
    };

    tElement.html(choiceTemplate);

    // linking function
    return function ($scope, element, attrs, AssessmentItemCtrl) {
        localScope.responseIdentifier = attrs["responseidentifier"];
        var updateAssessmentItem = function (orderedList) {
            var flattenedArray = [];

            if (orderedList)
                for (var i = 0; i < orderedList.length; i++) {
                    flattenedArray[i] = orderedList[i].identifier;
                }
            AssessmentItemCtrl.setResponse(localScope.responseIdentifier, flattenedArray);
            $scope.changed = true;
        };
        commonLinkFn.call(localScope, $scope, element, attrs, AssessmentItemCtrl, QtiUtils);

        // watch the response and set it to the responses list
        $scope.$watch('orderedList', function (newValue, oldValue) {
            console.log("Change ",newValue.length, oldValue.length);
            if ($scope.requireModification && (oldValue.length == 0 || newValue.length == 0)) {
                AssessmentItemCtrl.setResponse(that.responseIdentifier, []);
            } else {
                updateAssessmentItem(newValue);
            }
            $scope.noResponse = (!$scope.changed && $scope.showNoResponseFeedback);
        });

    };
}

var compilePlacementOrderInteraction = function (tElement, QtiUtils, $timeout) {

    var choiceTemplate = [
        '<div class="dragArea">',
        '<span ng-bind-html-unsafe="prompt" class="choice-prompt"></span>',
        '<div id="draggableItems" ng-class="{noResponse: noResponse}" style="z-index: 10">',
            '<draggable-item ng:repeat="item in items" obj="{{item}}" class="{{item.submittedClass}}" ng-bind-html-unsafe="item.content" > ',
        '</div>',

        '<div class="order-placement-destination-area">',
            '<placement-destination ng:repeat="item in emptyCorrectAnswers" index="{{$index}}" class="{{item.submittedClass}}" style="width: {{maxW}}px; height: {{maxH}}px; line-height: {{maxH}}px">',
            '<span ng-hide="hideNumbering">{{$index+1}}</span>',
            '</placement-destination>',
            '<div style="clear: both">Drag answers here</div>',
        '</div>',
        '</div>'

    ].join('');


    var localScope = {
        choices:  parseSimpleChoices(tElement),
        prompt:  parsePrompt(tElement)
    };

    // now modify the DOM
    tElement.html(choiceTemplate);

    // linking function
    return function ($scope, element, attrs, AssessmentItemCtrl) {

        localScope.responseIdentifier = attrs["responseidentifier"];
        var updateAssessmentItem = function (orderedList) {
            console.log("Updating Item");
            var flattenedArray = [];
            for (var i = 0; i < orderedList.length; i++) {
                if (orderedList[i])
                    flattenedArray.push(orderedList[i].identifier);
                else
                    flattenedArray.push('');
            }
            AssessmentItemCtrl.setResponse(localScope.responseIdentifier, flattenedArray);
            $scope.changed = (orderedList.length > 0);
        };

        commonLinkFn.call(localScope, $scope, element, attrs, AssessmentItemCtrl, QtiUtils);

        updateAssessmentItem([]);
        $scope.noResponse = true;
        // watch the response and set it to the responses list
        $scope.$watch('orderedList', function (newValue, oldValue) {
            if (newValue.length!=0 || oldValue.length!=0)
                updateAssessmentItem(newValue);
            $scope.noResponse = (!$scope.changed && $scope.showNoResponseFeedback);
        });


        var pollSize = function() {
            var maxW = 0, maxH = 0;
            var hasDimension = false;
            $(element).find('draggable-item').each(function (index) {
                if ($(this).width() > maxW) {
                    maxW = $(this).width();
                    hasDimension = true;
                }
                if ($(this).height() > maxH) {
                    maxH = $(this).height();
                    hasDimension = true;
                }
            });

            if (maxW<30) maxW = 30;
            if (maxH<30) maxH = 30;

            var hasGrown = false;

            if (maxW > $scope.maxW) {
                $scope.maxW = maxW;
                hasGrown = true;
            }

            if (maxH > $scope.maxH) {
                $scope.maxH = maxH;
                hasGrown = true;
            }

            if (hasGrown || !hasDimension)
                $timeout(pollSize, 100);

        }

        $scope.maxW = 30;
        $scope.maxH = 30;
        pollSize();

        // set model to choices extracted from html
        $scope.orderedList = [];
        $scope.requireModification = true;
        $scope.emptyCorrectAnswers = [];
        var cn = attrs.cscorrectanswers;
        if (angular.isUndefined(cn)) {
            cn = localScope.choices.length;
            $scope.hideNumbering = true;
        }

        for (var ecntr=0; ecntr < cn; ecntr++)
            $scope.emptyCorrectAnswers.push(ecntr);

    };
}


var commonLinkFn = function($scope, element, attrs, AssessmentItemCtrl, QtiUtils) {
    $scope.prompt = this.prompt;
    $scope.items = this.choices;
    $scope.changed = false;

    var that = this;

    var setAllIncorrect = function () {
        applyCssNameToAll("order-incorrect");
    };

    var applyCssNameToAll = function (name) {
        for (var y = 0; y < $scope.items.length; y++) {
            $scope.items[y].submittedClass = name;
        }
    };

    var applyCss = function (correctResponse, ourResponse) {
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

    $scope.$watch('showNoResponseFeedback', function (newVal, oldVal) {
        $scope.noResponse = (!$scope.changed && $scope.showNoResponseFeedback);
    });


    $scope.$on('resetUI', function (event) {
        applyCssNameToAll("");
    });

    $scope.$watch('itemSession.sessionData.correctResponses', function (responses) {
        if (!responses) return;
        if (!$scope.isFeedbackEnabled()) return;
        var correctResponse = responses[that.responseIdentifier];
        var ourResponse = QtiUtils.getResponseValue(that.responseIdentifier, $scope.itemSession.responses, []);
        applyCss(correctResponse, ourResponse)
    });

}





qtiDirectives.directive('orderinteraction',
    function (QtiUtils, $timeout) {
        return {
            restrict:'E',
            scope: true,
            require:'^assessmentitem',
            compile:function (tElement, tAttrs, transclude) {
                if (tAttrs.csorderingtype == "placement") {
                    return compilePlacementOrderInteraction(tElement, QtiUtils, $timeout);
                } else {
                    return compileNormalOrderInteraction(tElement, QtiUtils);
                }
            }
        }
    }
);


qtiDirectives.directive("draggableItem", function () {
    return {
        restrict:'E',
        link:function (scope, el, attrs, ctrl, $timeout) {
            $(el).draggable({
                containment: $(el).parents("div.dragArea"),
                start:function () {
                    angular.element(el).attr('rid', '');
                    scope.reverted = false;
                },
                revert:function (socketObj) {
                    if (socketObj === false) {
                        $(this).animate({left:0, top:0});
                        scope.reverted = true;
                    }
                    return false;
                },
                stop:function (ev, ui) {
                    if (scope.reverted) {
                        angular.element(el).attr('rid', '');
                    }
                    var items = [];
                    $(el).parent().children('draggable-item').each(function (index, element) {
                            if ($(element).attr('rid') != undefined && $(element).attr('rid').length>0) {
                                var rid = Number($(element).attr('rid'));
                                var liItem = scope.$eval($(element).attr('obj'));
                                if (!angular.isUndefined(liItem)) {
                                    liItem.ord = rid;
                                    items[rid] = liItem;
                                }
                            }
                        }
                    );

                    scope.$apply(function () {
                        scope.$parent.orderedList = items;
                    });

                }
            });
        }
    }
});

qtiDirectives.directive("placementDestination", function () {
    return {
        restrict:'E',
        link:function (scope, el, attrs, ctrl, $timeout) {
            $(el).droppable(
                {
                    hoverClass: "placing",
                    accept: function(dest) {
                        var hasDeployed = false;
                        $(el).parent().parent().find('draggable-item').each(function(idx, element) {
                            var rid = angular.element(element).attr('rid');
                            hasDeployed |= angular.element(el).attr('index') == rid;
                        });
                        return !hasDeployed;
                    },
                    drop: function(e, ui) {
                        var draggableElement = ui.draggable;

                        $(draggableElement).position({
                            my:        "center",
                            at:        "center",
                            of:        $(e.target),
                            collision: "none"
                        });

                        angular.element(draggableElement).attr('rid', angular.element(el).attr('index'));
                    }
                }
            );
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

            scope.$watch("formDisabled", function (newValue) {
                $(el).sortable("option", "disabled", newValue === true);
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


