/**
 * Interaction for ordering a set of choices
 */

var parseSimpleChoices = function (element, dontShuffle) {
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

        choices.push({content: elem.html(), identifier: identifier, label: elem.attr('label')});
    }

    if (!dontShuffle) choices.shuffle(fixedIndexes);
    return choices;
}

var parsePrompt = function (element) {
    // get the prompt element if present
    // it supports embedded html
    var result = element.find("prompt");
    var prompt = "";
    if (result.length == 1) {
        var promptElem = angular.element(result[0]);
        prompt = promptElem.html();
    }
    return prompt;
};


var compileNormalOrderInteraction = function (shuffle, tElement, QtiUtils) {

    var choiceTemplate = [
        '<span ng-bind-html-unsafe="prompt" class="choice-prompt"></span>',
        '<div sortable="" class="sortable-body" ng-class="{noResponse: noResponse}">',
        '<div ng:repeat="item in items" obj="{{item}}"> ',
        '<span class="sortable-item {{item.submittedClass}}" ng-bind-html-unsafe="item.content"></span>',
        ' </div>',
        '</div>'].join('');


    var localScope = {
        choices: parseSimpleChoices(tElement, !shuffle),
        prompt: parsePrompt(tElement)
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
        };
        commonLinkFn.call(localScope, $scope, element, attrs, AssessmentItemCtrl, QtiUtils);
        $scope.requireModification = (attrs.csRequiremodification != undefined) ? attrs.csRequiremodification === 'true' : true;

        // watch the response and set it to the responses list
        $scope.$watch('orderedList', function (newValue, oldValue) {
            if (!$scope.requireModification || oldValue) {
                updateAssessmentItem(newValue);
                $scope.changed = true;
            }
            $scope.noResponse = (!$scope.changed && $scope.showNoResponseFeedback);
        });

    };
}

var compilePlacementOrderInteraction = function (shuffle, tElement, isVertical, QtiUtils, $timeout) {


    var choiceTemplate = isVertical ?
        [
            '<div class="dragArea">',
            '<span ng-bind-html-unsafe="prompt" class="choice-prompt"></span>',
            '<div class="verticalHolder">',
            '<div id="draggableItems" class="vertical" ng-class="{noResponse: noResponse}" style="z-index: 10">',
            '<span class="draggable-item {{item.submittedClass}}" ng:repeat="item in items" obj="{{item}}" ng-bind-html-unsafe="item.content" /> ',
            '</div>',

            '<div class="order-placement-destination-area-vertical" style="width: {{maxW}}px">',
            '<div style="clear: both; margin-bottom: 10px">Place answers here</div>',
            '<span class="placement-destination" ng:repeat="item in emptyCorrectAnswers" index="{{$index}}" class="{{item.submittedClass}}" style="width: {{maxW}}px; height: {{maxH}}px">',
            '<span class="placement-destination-inner" style="width: {{maxW}}px; height: {{maxH}}px"></span>',
            '<div class="numbering" ng-hide="hideNumbering"><span class="number">{{$index+1}}</span></div>',
            '</span>',
            '<div style="clear: both"></div>',
            '</div><div style="clear: both"></div>',
            '</div>'

        ].join('')
        :

        [
            '<div class="dragArea">',
            '<span ng-bind-html-unsafe="prompt" class="choice-prompt"></span>',
            '<div id="draggableItems" ng-class="{noResponse: noResponse}" >',
            '<span class="draggable-item {{item.submittedClass}}" ng:repeat="item in items" obj="{{item}}" ng-bind-html-unsafe="item.content"></span>',
            '</div>',

            '<div class="order-placement-destination-area">',
            '<div style="clear: both">Place answers here</div>',
            '<span class="placement-destination" ng:repeat="item in emptyCorrectAnswers" index="{{$index}}" class="{{item.submittedClass}}" style="width: {{maxW}}px; height: {{maxH}}px">',
            '<span class="placement-destination-inner" style="width: {{maxW}}px; height: {{maxH}}px"><div class="numbering" ng-hide="hideNumbering"><span class="number">{{item.label}}</span></div></span>',
            '',
            '</span>',
            '</div>',
            '</div>'

        ].join('');


    var localScope = {
        choices: parseSimpleChoices(tElement, !shuffle),
        originalChoices: parseSimpleChoices(tElement, true),
        prompt: parsePrompt(tElement)
    };


    // now modify the DOM
    tElement.html(choiceTemplate);

    // linking function
    return function ($scope, element, attrs, AssessmentItemCtrl) {

        localScope.responseIdentifier = attrs["responseidentifier"];

        AssessmentItemCtrl.registerInteraction(element.attr('responseIdentifier'), element.find('.prompt').html(), "sequencing");


        var updateAssessmentItem = function (orderedList) {
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
            if (newValue.length != 0 || oldValue.length != 0)
                updateAssessmentItem(newValue);
            $scope.noResponse = (!$scope.changed && $scope.showNoResponseFeedback);
        });


        var pollSize = function () {
            var maxW = 0, maxH = 0;
            var hasDimension = false;
            $(element).find('.draggable-item').each(function (index) {
                if ($(this).width() > maxW) {
                    maxW = Math.min($(this).width() + 30, 200);
                    hasDimension = true;
                }
                if ($(this).height() > maxH) {
                    maxH = Math.min($(this).height() + 30, 200);
                    hasDimension = true;
                }
            });

            if (maxW < 30) maxW = 30;
            if (maxH < 30) maxH = 30;

            var sizeHasChanged = false;

            if (maxW != $scope.maxW) {
                $scope.maxW = maxW;
                sizeHasChanged = true;
            }

            if (maxH != $scope.maxH) {
                $scope.maxH = maxH;
                sizeHasChanged = true;
            }

            // If the elements have no dimension yet (i.e. pre-render phase),
            // or the size changed from previous poll let's keep polling
            if (sizeHasChanged || !hasDimension) {
                $scope.pollCounter = 0;
            } else {
                if (isVertical)
                    $(element).find('#draggableItems').width(maxW + 50);
                else
                    $(element).find('#draggableItems').height(maxH + 50);
            }

            $scope.pollCounter++;
            // If the size has not changed for 2 seconds we consider rendering done
            if ($scope.pollCounter > 10)
                return;


            $timeout(pollSize, 200);

        }

        $scope.maxW = 30;
        $scope.maxH = 30;
        $scope.pollCounter = 0;
        pollSize();

        // set model to choices extracted from html
        $scope.orderedList = [];
        $scope.requireModification = true;
        $scope.emptyCorrectAnswers = [];
        var cn = attrs.cscorrectanswers;
        $scope.hideNumbering = false;
        if (angular.isUndefined(cn)) {
            cn = localScope.choices.length;
            $scope.hideNumbering = true;
        }

        for (var ecntr = 0; ecntr < cn; ecntr++) {
            var o = {};
            o.label = localScope.originalChoices[ecntr].label;
            if (o.label == undefined) o.label = String(ecntr + 1);
            $scope.emptyCorrectAnswers.push(o);
        }

    };
}


var commonLinkFn = function ($scope, element, attrs, AssessmentItemCtrl, QtiUtils) {

    $scope.prompt = this.prompt;
    $scope.changed = false;
    $scope.orderedList = $scope.items = this.choices;
    $scope.defaultItems = angular.copy(this.choices);
    $scope.responseIdentifier = attrs["responseidentifier"];
    $scope.requireModification = (attrs.csRequiremodification != undefined) ? attrs.csRequiremodification === 'true' : true;

    var that = this;

    var setAllIncorrect = function (style) {
        applyCssNameToAll(style);
    };

    var applyCssNameToAll = function (name) {
        for (var y = 0; y < $scope.items.length; y++) {
            $scope.items[y].submittedClass = name;
        }
    };

    var applyCss = function (correctResponse, ourResponse) {
        setAllIncorrect(correctResponse.length == 0 ? "order-unknown" : "order-incorrect");
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


    $scope.$on('highlightUserResponses', function () {

        var getItemById = function (id) {
            for (var i = 0; i < $scope.items.length; i++) {
                if ($scope.items[i].identifier == id) {
                    return $scope.items[i];
                }
            }
            return null;
        };

        var buildFromResponse = function (ids) {
            var out = [];
            for (var i = 0; i < ids.length; i++) {
                var item = getItemById(ids[i]);
                if (item) {
                    out.push(item);
                }
            }
            return out;
        };

        var value = QtiUtils.getResponseValue($scope.responseIdentifier, $scope.itemSession.responses, "");
        $scope.highlightingUserResponse = true;
        if (value) {
            $scope.orderedList = buildFromResponse(value);
        }
    });


    $scope.$watch('itemSession.sessionData.correctResponses', function (responses) {
        if (!responses) return;
        var correctResponse = QtiUtils.getResponseValue(that.responseIdentifier, responses, []);
        var ourResponse = QtiUtils.getResponseValue(that.responseIdentifier, $scope.itemSession.responses, []);
        applyCss(correctResponse, ourResponse)
    });

    $scope.$on('unsetSelection', function (event) {
        for (var x = 0; x < $scope.items.length; x++) {
            $scope.items[x] = {content: $scope.defaultItems[x].content, identifier: $scope.defaultItems[x].identifier};
        }
        $scope.orderedList = $scope.items;
        if (typeof(MathJax) != "undefined") {
            setTimeout(function () {
                MathJax.Hub.Queue(["Typeset", MathJax.Hub, element[0]]);
            }, 0);
        }

    });

};


angular.module('qti.directives').directive('orderinteraction',
    function (QtiUtils, $timeout) {
        return {
            restrict: 'E',
            scope: true,
            require: '^assessmentitem',
            controller: function($scope, $attrs) {
                $scope.respId = $attrs["responseidentifier"];
            },
            compile: function (tElement, tAttrs, transclude) {
              var shuffle = (tAttrs["shuffle"] == 'true');
              if (tAttrs.csorderingtype == "placement") {
                    var isVertical = tAttrs.orientation == "vertical";
                    return compilePlacementOrderInteraction(shuffle, tElement, isVertical, QtiUtils, $timeout);
                } else {
                    return compileNormalOrderInteraction(shuffle, tElement, QtiUtils);
                }
            }
        }
    }
);


angular.module('qti.directives').directive("draggableItem", function ($rootScope) {
    return {
        restrict: 'C',
        require: '^orderinteraction',

        link: function (scope, el, attrs, ctrl, $timeout) {
            scope.$watch("formSubmitted", function (newValue) {
                $(el).draggable("option", "disabled", newValue === true);
            });

            $(el).draggable({
                containment: $(el).parents("div.dragArea"),
                start: function () {
                    scope.srcRid = angular.element(el).attr('rid');
                    angular.element(el).removeClass("order-correct").removeClass("order-incorrect");
                    angular.element(el).attr('rid', '');
                    scope.reverted = false;
                },
                revert: function (socketObj) {
                    if (socketObj === false) {
                        if (scope.srcRid == undefined || scope.srcRid == '') {
                            return true;
                        }
                        $(el).parents('.dragArea').find('#draggableItems').append($(this));
                        $(this).css('left', 0);
                        $(this).css('top', 0);
                        $(this).removeClass('placed');
                        $(el).find('.numbering').show();
                        scope.reverted = true;
                        $rootScope.$broadcast("reverted", scope.respId, scope.srcRid);

                    }
                    return false;
                },
                stop: function (ev, ui) {
                    var items = [];
                    $(el).parents('.dragArea').find('.draggable-item').each(function (index, element) {
                            if ($(element).attr('rid') != undefined && $(element).attr('rid').length > 0) {
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

angular.module('qti.directives').directive("placementDestination", function () {
    return {
        restrict: 'C',
        require: '^orderinteraction',
        link: function (scope, el, attrs, ctrl, $timeout) {
            scope.$on("reverted", function(a, respId, idx) {
                if (!scope.hideNumbering && respId == scope.respId && angular.element(el).attr('index') == idx)  {
                    $(el).find('.numbering').show();
                }
            });
            $(el).droppable(
                {
                    hoverClass: "placing",
                    accept: function (dest) {
                        var hasDeployed = false;
                        $(el).parent().parent().find('.draggable-item').each(function (idx, element) {
                            var rid = angular.element(element).attr('rid');
                            hasDeployed |= angular.element(el).attr('index') == rid;
                        });
                        return !hasDeployed;
                    },
                    drop: function (e, ui) {
                        var draggableElement = ui.draggable;

                        $(e.target).find('.placement-destination-inner').append(draggableElement);

                        $(draggableElement).css('left', '0');
                        $(draggableElement).css('top', '0');

                        $(draggableElement).addClass('placed');
                        $(el).find('.numbering').hide();

                        angular.element(draggableElement).attr('rid', angular.element(el).attr('index'));
                    }
                }
            );
        }
    }
});


angular.module('qti.directives').directive("sortable", function () {
    return {
        // todo look into isolate scope so orderedList is not on global scope, tried it but was having trouble
        link: function (scope, el, attrs, ctrl, $timeout) {

            var stopParent = null;

            var buildItemsList = function ($node) {
                var items = [];
                $node.children('div').each(function (index) {
                    var liItem = scope.$eval($(this).attr('obj'));
                    items.push(liItem);
                });

                return items;
            };

            scope.$watch("formSubmitted", function (newValue) {
                $(el).sortable("option", "disabled", newValue === true);
            });

            scope.$watch("orderedList", function (newValue) {
              /**
               * Only set the items if we are highlighting a user response aka the item is closed
               * Otherwise this will cause unpredictable behaviour
               */
               if( scope.highlightingUserResponse ){
                 scope.items = newValue;
               }
            });

            $(el).sortable({
                items: 'div:not(:has(div.complete))',
                stop: function (event, ui) {
                    stopParent = $(ui.item).parent();
                    var items = buildItemsList(stopParent);
                    scope.$apply(function () {
                        scope.orderedList = items;
                    });

                },
                connectWith: '.sortable-linked',
                placeholder: 'ui-state-highlight',
                forcePlaceholderSize: true,
                axis: 'y',
                dropOnEmpty: true
            });
        }
    }
});


