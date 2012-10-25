/**
 * Interaction for ordering a set of choices
 */

var compileNormalOrderInteraction = function (tElement, QtiUtils) {

    var choiceTemplate = [
        '<span ng-bind-html-unsafe="prompt" class="choice-prompt"></span>',
        '<div sortable="" class="sortable-body" ng-class="{noResponse: noResponse}">',
        '<div ng:repeat="item in items" obj="{{item}}"> ',
        '<span class="sortable-item {{item.submittedClass}}" ng-bind-html-unsafe="item.content"></span>',
        ' </div>',
        '</div>'].join('');


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

        if (fixed) {
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
        $scope.requireModification = (attrs.csRequiremodification != undefined) ? attrs.csRequiremodification === 'true' : true;


        var updateAssessmentItem = function (orderedList) {
            var flattenedArray = [];
            for (var i = 0; i < orderedList.length; i++) {
                flattenedArray[i] = orderedList[i].identifier;
            }
            AssessmentItemCtrl.setResponse(responseIdentifier, flattenedArray);
            $scope.changed = true;
        };

        $scope.$watch('showNoResponseFeedback', function (newVal, oldVal) {
            $scope.noResponse = (!$scope.changed && $scope.showNoResponseFeedback);
        });

        // watch the response and set it to the responses list
        $scope.$watch('orderedList', function (newValue, oldValue) {
            if ($scope.requireModification && (oldValue.length == 0 || newValue.length == 0)) {
                AssessmentItemCtrl.setResponse(responseIdentifier, []);
            } else {
                updateAssessmentItem(newValue);
            }
            $scope.noResponse = (!$scope.changed && $scope.showNoResponseFeedback);
        });

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

        /**
         * Reset the ui.
         */
        $scope.$on('resetUI', function (event) {
            applyCssNameToAll("");
        });


        $scope.$watch('itemSession.sessionData.correctResponses', function (responses) {
            if (!responses) return;
            if (!$scope.isFeedbackEnabled()) return;
            var correctResponse = responses[responseIdentifier];
            var ourResponse = QtiUtils.getResponseValue(responseIdentifier, $scope.itemSession.responses, [])
            applyCss(correctResponse, ourResponse)
        });

    };
}

var compilePlacementOrderInteraction = function (tElement, QtiUtils, $timeout) {

    var choiceTemplate = [
        '<span ng-bind-html-unsafe="prompt" class="choice-prompt"></span>',
        '<div id="draggableItems" ng-class="{noResponse: noResponse}" style="z-index: 10">',
            '<draggable-item ng:repeat="item in items" obj="{{item}}" class="sortable-item placable-item {{item.submittedClass}}" ng-bind-html-unsafe="item.content" > ',
        '</div>',

        '<div style="background-color: red; width:400px; z-index: 0">',
            '<placement-destination ng:repeat="item in items" index="{{$index}}" class="span1 placement-destination {{item.submittedClass}}" style="width: {{maxW}}px; height: {{maxH}}px">',
            '{{index}}',
            '</placement-item>',
        '</div>',
        '<div style="clear: both">fdifjds</div>',

        '{{orderedList}}',

    ].join('');


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

        if (fixed) {
            fixedIndexes.push(i);
        }

        choices.push({content:elem.html(), identifier:identifier});
    }

    choices.shuffle(fixedIndexes);

    // now modify the DOM
    tElement.html(choiceTemplate);
    // linking function
    return function ($scope, element, attrs, AssessmentItemCtrl) {
        $timeout(function() {
            var maxW = 0, maxH = 0;
            $(element).find('draggable-item').each(function (index) {
                console.log(index, $(this).width());
                if ($(this).width() > maxW)
                    maxW = $(this).outerWidth();

                if ($(this).height() > maxH)
                    maxH = $(this).outerHeight();

            });

            $scope.maxW = maxW;
            $scope.maxH = maxW;
        }, 1);
        var responseIdentifier = attrs["responseidentifier"];
        // set model to choices extracted from html
        $scope.orderedList = [1,2];
        $scope.result = [];
        $scope.prompt = prompt;
        $scope.items = choices;
        $scope.changed = false;

        $scope.maxW = 32;
        $scope.maxH = 30;

        var updateAssessmentItem = function (orderedList) {
            var flattenedArray = [];
            for (var i = 0; i < orderedList.length; i++) {
                flattenedArray[orderedList[i].ord] = orderedList[i].identifier;
            }
//            AssessmentItemCtrl.setResponse(responseIdentifier, flattenedArray);
            $scope.changed = true;
        };

        $scope.$watch('showNoResponseFeedback', function (newVal, oldVal) {
            $scope.noResponse = (!$scope.changed && $scope.showNoResponseFeedback);
        });

        // watch the response and set it to the responses list
        $scope.$watch('orderedList', function (newValue, oldValue) {
            console.log("Ordered List Changed");
            if ($scope.requireModification && (oldValue.length == 0 || newValue.length == 0)) {
                AssessmentItemCtrl.setResponse(responseIdentifier, []);
            } else {
                updateAssessmentItem(newValue);
            }
            $scope.noResponse = (!$scope.changed && $scope.showNoResponseFeedback);
        });

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

        $scope.$on('resetUI', function (event) {
            applyCssNameToAll("");
        });


        $scope.$watch('itemSession.sessionData.correctResponses', function (responses) {
            if (!responses) return;
            if (!$scope.isFeedbackEnabled()) return;
            var correctResponse = responses[responseIdentifier];
            var ourResponse = QtiUtils.getResponseValue(responseIdentifier, $scope.itemSession.responses, [])
            applyCss(correctResponse, ourResponse)
        });

    };

}



var orderInteraction =
    function (QtiUtils, $timeout) {
        return {
            restrict:'E',
            scope: true,
            require:'^assessmentitem',
            controller: function() {

            },
            compile:function (tElement, tAttrs, transclude) {
                console.log(tAttrs.csorderingtype);
                if (tAttrs.csorderingtype == "placement") {
                    return compilePlacementOrderInteraction(tElement, QtiUtils, $timeout);
                } else {
                    return compileNormalOrderInteraction(tElement, QtiUtils);
                }
            }

        }
    }

qtiDirectives.directive('orderinteraction', orderInteraction);


qtiDirectives.directive("draggableItem", function () {
    return {
        restrict:'E',
        link:function (scope, el, attrs, ctrl, $timeout) {
            console.log("sorting");

            $(el).draggable({
                start: function() {
                    angular.element(el).attr('rid','');
                    scope.reverted = false;
                },
                revert: function(socketObj) {
                    if (socketObj === false) {
                        $(this).animate({left:0, top: 0});
                        scope.reverted = true;
                    }
                    return false;
                },
                stop: function(ev, ui) {
                    if (scope.reverted) {
                        console.log("TA: "+angular.element(el).attr('rid'));
                        angular.element(el).attr('rid','');
                    } else {
                    }
                    var items = [];
                    $(el).parent().children('draggable-item').each(function (index, element) {
                        var rid = $(element).attr('rid');
                        var liItem = scope.$eval($(element).attr('obj'));

                        if (!angular.isUndefined(liItem)) {
                            liItem.ord = index;
                            items[rid] = liItem;
                        }
                    });
                    console.log(items);
                    console.log("Stopped "+scope.reverted);
                }
            });
        }
    }
});

qtiDirectives.directive("placementDestination", function () {
    return {
        restrict:'E',
        link:function (scope, el, attrs, ctrl, $timeout) {
            console.log("sorting");


            var buildItemsList = function ($node) {
                var items = [];
                $node.children('placement-destination').each(function (index) {
                    var liItem = scope.$eval($(this.droppedItem).attr('obj'));
                    if (!angular.isUndefined(liItem)) {
                        liItem.ord = index;
                        items.push(liItem);
                    }
                });

                return items;
            };
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
                            my:        "left top",
                            at:        "left top",
                            of:        $(e.target),
                            collision: "fit"
                        });

                        angular.element(draggableElement).attr('rid', angular.element(el).attr('index'));
                        var list = $(this).parent();
                        scope.$apply(function () {
                            scope.$parent.orderedList = buildItemsList(list);
                        });
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


