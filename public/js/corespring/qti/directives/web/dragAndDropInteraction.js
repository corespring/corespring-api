angular.module('qti.directives').directive("draganddropinteraction", function (QtiUtils) {
  return {
    restrict: 'E',
    require: '^assessmentitem',
    scope: true,
    controller: function ($scope, $attrs) {
      $scope.responseIdentifier = $attrs.responseidentifier;
      $scope.indexes = {answerIndex: 0, targetIndex: 0};
      $scope.listAnswers = [];
      $scope.originalListAnswers = [];
      $scope.listTargets = [];
      $scope.targetMap = {};
      $scope.contentMap = {};
      $scope.dragging = {};
      $scope.canDrag = true;
      $scope.maxWidth = 50;
      $scope.maxHeight = 20;
      $scope.stateStack = [];
      $scope.sizeMap = {};

      $scope.getTargetIndex = function (target) {
        return $scope.targetMap[target];
      };

      $scope.getContentForId = function (id) {
        return $scope.contentMap[id];
      };

      $scope.showSolution = function() {
        $scope.solutionVisible = true;
      };

      $scope.hideSolution = function() {
        $scope.solutionVisible = false;
      };

      $scope.propagateDimension = function (id, w, h) {
        $scope.sizeMap[id] = {w: w, h: h};
        var maxW = 0, maxH = 0;
        _.each($scope.sizeMap, function(size) {
           if (size.w > maxW) maxW = size.w;
           if (size.h > maxH) maxH = size.h;
        });
        if (maxW != $scope.maxWidth) {
          $scope.maxWidth = maxW;
        }
        if (maxH != $scope.maxHeight) {
          $scope.maxHeight = maxH;
        }
      };

      $scope.undo = function () {
        if ($scope.stateStack.length <= 1) return;
        $scope.stateStack.pop();
        var state = _.last($scope.stateStack);
        $scope.listAnswers = QtiUtils.deepCopy(state.answers);
        $scope.listTargets = QtiUtils.deepCopy(state.targets);
        $scope.initMathML(0);
      };

      $scope.resetClick = function () {
        var i;
        for (i = 0; i < $scope.listTargets.length; i++) {
          $scope.listTargets[i] = _.isArray($scope.listTargets[i]) ? [] : {};
        }
        for (i = 0; i < $scope.listAnswers.length; i++) {
          $scope.listAnswers[i] = $scope.originalListAnswers[i];
        }
        $scope.stateStack = [];
        $scope.initMathML(0);
      };

      $scope.$on('unsetSelection', function (event) {
        $scope.resetClick();
        $scope.isShowSolutionButtonVisible = false;
        $scope.canDrag = true;
      });

      $scope.dropCallback = function (event, ui) {
        $scope.initMathML(0);

        //Remove empty objects from multiple (array based) targets
        _.each($scope.listTargets, function (el, idx) {
          if (_.isArray(el)) {
            $scope.listTargets[idx] = _.filter(el, function (el2) {
              return el2.id;
            });
          }
        });
      };
    },

    compile: function (elem, attrs) {
      var originalHtml = elem.html();

      // Generates the html to be shown on the solution popup. It's basically a copy of the area that contains the targets
      var getAnswerAreaTemplate = function (fromHtml) {
        var draggableChoiceRegexp = /(<:*answerArea[\s\S]*?>[\s\S]*?<\/:*answerArea>)/gmi;
        var html = fromHtml.replace(/<:*landingPlace([\s\S]*?)>/gmi, "<landingSolution$1>").replace(/<\/:*landingPlace>/gmi, "</landingSolution>");
        var answerAreaMatch = draggableChoiceRegexp.exec(html);
        var solutionHtml = (answerAreaMatch && answerAreaMatch.length > 0) ? answerAreaMatch[0] : removeAnswerNodes(html);
        return "<div ui-modal data-dynamic='true' ng-model='solutionVisible' class='drag-and-drop-solution-modal'><span class='close-button' ng-click='hideSolution()' style='z-index: 10'></span><h1>Answer</h1>" + solutionHtml + "<a ng-click='hideSolution()'>See your answer</a></div>";
      };

      var removePromptNode = function (fromHtml) {
        return fromHtml.replace(/<:*prompt>([\s\S]*?)<\/:*prompt>/gim, "");
      };

      // Removes all the draggable choices from the html. This is a fallback solution for when there is no <answerArea> defined in the
      // question xml
      var removeAnswerNodes = function (str) {
        return str.replace(/<:*draggableChoiceGroup[\s\S]*?>[\s\S]*?<\/:*draggableChoiceGroup>/gmi, "").
          replace(/<:*draggableChoice[\s\S]*?>[\s\S]*?<\/:*draggableChoice>/gmi, "");
      };

      var topButtonRowHtml = ["<div class='button-row'>",
        "<button class='btn pull-right' ng-click='resetClick()' ng-show='canDrag'>Start Over</button>",
        "<button class='btn pull-right' style='margin-right: 5px' ng-click='undo()' ng-show='canDrag' ng-disabled='stateStack.length < 2'>Undo</button></div>"
      ].join("");

      var solutionButtonHtml = "<div class='button-row' style='margin-top: 25px' ng-show='isShowSolutionButtonVisible'><a class='pull-right solution-link' ng-click='showSolution()'>See Correct Answer</a></div>";

      elem.html(
        [
          getAnswerAreaTemplate(originalHtml),
          "<div class='drag-and-drop-interaction'>",
          QtiUtils.getPromptSpan(originalHtml),
          topButtonRowHtml,
          removePromptNode(originalHtml),
          solutionButtonHtml,
          "</div>"
        ].join("")
      );

      return function link($scope, element, attrs, AssessmentItemCtrl) {
        $scope.$watch("formSubmitted", function (newValue) {
          if (newValue != undefined) $scope.canDrag = !newValue;
        });

        $scope.$watch(function () {
          return _.reduce($scope.listTargets,
            function (acc, el) {
                if (_.isArray(el))
                  return acc + _.pluck(el, "id").join(",");
                else
                  return acc + el.id + ",";
            }, ""
          );
        }, function () {
            var response = [];
            for (var target in $scope.targetMap) {
              var idx = $scope.targetMap[target];
              var targetElement = $scope.listTargets[idx];
              var str = _.isArray(targetElement) ? _.pluck(targetElement, "id").join("|") : targetElement.id;

              if (!_.isEmpty(str))
                response.push(target + ":" + str);

            }
            var state = {answers: QtiUtils.deepCopy($scope.listAnswers), targets: QtiUtils.deepCopy($scope.listTargets)};

            if (!_.isEqual(state, _.last($scope.stateStack))) {
              $scope.stateStack.push(state);
            }
            AssessmentItemCtrl.setResponse($scope.responseIdentifier, response);
        });

        $scope.$on('highlightUserResponses', function () {
          var value = QtiUtils.getResponseValue($scope.responseIdentifier, $scope.itemSession.responses, "");
          var response = _.find($scope.itemSession.responses, function(r) {
            return r.id == $scope.responseIdentifier;
          });

          if (response && response.outcome && !response.outcome.isCorrect)
            $scope.isShowSolutionButtonVisible = $scope.highlightCorrectResponse();

          _.each(value, function (v) {
            var arr = v.split(":");
            var target = arr[0];
            var answerIdList = arr[1].split("|");
            var targetIndex = $scope.getTargetIndex(target);
            $scope.listTargets[targetIndex] =
              _.isArray($scope.listTargets[targetIndex]) ?
                _.map(answerIdList,
                  function (answerId) {
                    return {id: answerId, title: $scope.getContentForId(answerId)};
                  }) : {id: answerIdList[0]};

            $scope.listAnswers = _.map($scope.listAnswers, function (answer) {
              if (answerIdList.indexOf(answer.id) >= 0) return {}; else return answer;
            });
          });
        });
      }
    }
  }
});

angular.module('qti.directives').directive("draggablechoice", function () {
  return {
    restrict: 'E',
    require: "^draganddropinteraction",
    replace: true,
    scope: true,
    compile: function (tElement, tAttrs, transclude) {
      var originalContent = tElement.html();
      var copyOnDrag = tAttrs.copyondrag == "true";
      var placeHolder = copyOnDrag ? "'keep'" : true;
      var helper = copyOnDrag ? "'clone'" : "''";

      var template = [
        '<div class="answerContainer {{correctClass}} {{phClass}}" data-drop="true" ng-model="listAnswers" data-jqyoui-options="dropOptions" jqyoui-droppable="{index: {{answerIndex}}, onDrop: \'dropCallback\'}">',
        ' <div class="contentElement" ng-bind-html-unsafe="itemContent.title"',
        ' data-drag="{{canDrag}}" jqyoui-draggable="{index: {{answerIndex}},placeholder:' + placeHolder + ',animate:false,onStart:\'startCallback\',onStop:\'stopCallback\'}"',
        ' data-jqyoui-options="{revert: \'invalid\',helper: ' + helper + '}" ng-model="listAnswers" ng-show="listAnswers[answerIndex].id"></div>',
        ' <div class="clearfix"></div>',
        '</div>',

        '<div class="sizerHolder">',
        originalContent,
        '</div>'].join(" ");

      tElement.html(template);

      return function ($scope, el, attrs) {
        $scope.answerIndex = $scope.indexes.answerIndex++;
        $scope.contentMap[attrs.identifier] = originalContent;
        $scope.listAnswers.push({id: attrs.identifier});
        $scope.originalListAnswers.push({id: attrs.identifier, title: originalContent});
        $scope.width = attrs.width ? attrs.width : "50px";
        $scope.height = attrs.height ? attrs.height : "50px";
        $scope.copyOnDrag = attrs.copyondrag == "true";
        $scope.placeholderClass = attrs.placeholderClass;
        var lastW, lastH;
        setInterval(function () {

          if(!$scope.$$phase) {

            $scope.$apply(function () {

              var w = $(el).find('.sizerHolder').width();
              var h = $(el).find('.sizerHolder').height();

              if (lastW != w || lastH != h) {
                $scope.propagateDimension(attrs.identifier, w, h);
              }

              lastW = w;
              lastH = h;
            });
          }
        }, 1000);

        $scope.dropOptions = {
          accept: function () {
            return $scope.dragging.id == attrs.identifier;
          }
        };

        $scope.startCallback = function () {
          $scope.dragging.id = $scope.listAnswers[$scope.answerIndex].id;
          $scope.dragging.draggingFromAnswer = true;
          $scope.dragging.fromTarget = undefined;



          $scope.$apply(function () {
            $scope.phClass = $scope.placeholderClass;
          });
        };

        $scope.stopCallback = function () {
          $scope.$apply(function () {
            $scope.phClass = "";
            $scope.dragging.draggingFromAnswer = false;
          });
        };

        $scope.$watch("listAnswers[" + $scope.answerIndex + "]", function () {
          $scope.itemContent = $scope.listAnswers[$scope.answerIndex];
          $scope.itemContent.title = $scope.contentMap[$scope.listAnswers[$scope.answerIndex].id];
        });

        $scope.$watch("maxWidth + maxHeight", function () {
          $(el).find('.contentElement').width($scope.maxWidth);
          $(el).find('.contentElement').height($scope.maxHeight);
        });


      }
    }
  }
});

angular.module('qti.directives').directive("landingplace", function (QtiUtils) {
  return {
    restrict: 'E',
    require: "^draganddropinteraction",
    replace: true,
    scope: true,
    compile: function (el, attrs) {
      var isMultiple = attrs.cardinality == 'multiple' ||  attrs.cardinality == 'ordered';
      var expandHorizontally = attrs.expand == 'horizontal';
      var style = expandHorizontally ? "min-height: {{lpHeight}}px; min-width: {{width}}px" : "min-height: {{lpHeight}}px; width: {{width}}px";

      var originalHtml = el.html();
      var classAttrs = attrs['class'] || "";

      var template = isMultiple ?
        [
          '<div style="'+style+'" class="landing {{correctClass}} '+classAttrs+'" data-drop="true" ng-model="listTargets[targetIndex]"',
          'jqyoui-droppable="{onDrop: \'dropCallback\', onOver: \'overCallback\', multiple: true}" data-jqyoui-options="dropOptions">',
          '<div class="landingLabelHolder" ng-show="label">',
          ' <span class="landingLabel" style="">{{label}}</span>',
          '</div>',
          '<div ui-sortable="sortableOptions" ng-model="listTargets[targetIndex]">',
          ' <div ng-repeat="item in listTargets[targetIndex]" class="contentElement"',
          ' jqyoui-draggable="{index: {{$index}}, placeholder:true, animate:false, onStart: \'startCallback\'}"',
          ' data-jqyoui-options="draggableOptions" ng-model="listTargets[targetIndex]" ng-show="!_.isEmpty(item.title)" ng-bind-html-unsafe="item.title" data-id="{{item.id}}"></div>',
          '<div class="clearfix"></div>',
          '</ul>',
          originalHtml,
          '<span class="floating-icon {{correctClass}}"></span>',
          '</div>'].join(" ")
        :
        [
          '<div class="landing {{correctClass}} '+classAttrs+'" style="'+style+'" data-drop="true" ng-model="listTargets" ',
          'jqyoui-droppable="{index: {{targetIndex}}, onDrop: \'dropCallback\', onOver: \'overCallback\', multiple: false}" data-jqyoui-options="dropOptions">',
          '<div class="landingLabelHolder"  ng-show="label">',
          ' <span class="landingLabel" style="">{{label}}</span>',
          '</div>',
          ' <div class="contentElement"',
          ' data-drag="{{canDrag}}" jqyoui-draggable="{index: {{targetIndex}}, placeholder:true, animate:false, onStart: \'startCallback\'}"',
          ' data-jqyoui-options="draggableOptions" ng-model="listTargets" ng-show="itemContent.title" ng-bind-html-unsafe="itemContent.title" data-id="{{itemContent.id}}"></div>',
          '<div class="clearfix"></div>',
          originalHtml,
          '<span class="floating-icon {{correctClass}}"></span>',
          '</div>'
        ].join(" ");

      el.html(template);

      return function ($scope, el, attrs) {
        $scope.correctClass = "";
        $scope.isMultiple = attrs.cardinality == 'multiple' ||  attrs.cardinality == 'ordered';
        $scope.isOrdered = attrs.cardinality == 'ordered';

        var columnsPerRow = attrs.columnsperrow || 3;
        var defaultWidth = $scope.isMultiple ? columnsPerRow * 50 : "50";
        $scope.width = defaultWidth;

        $scope.targetIndex = $scope.indexes.targetIndex++;
        $scope.listTargets.push($scope.isMultiple ? [] : {});
        $scope.targetMap[attrs.identifier] = $scope.targetIndex;
        $scope.label = attrs.label;

        $scope.overCallback = function() {
          $scope.dragging.isOut = false;
        };

        $scope.outCallback = function() {
          $scope.dragging.isOut = true;
        };

        $scope.sortableOptions = {
          start: function(ev, b) {
            $scope.dragging.id = $(b.item).attr('data-id');
            $scope.dragging.draggingFromAnswer = false;
            $scope.dragging.fromTarget = attrs.identifier;
          },
          beforeStop: function() {
            if ($scope.dragging.isOut) {
              $scope.revertFunction();
            }
            $scope.dragging.fromTarget = undefined;
          },
          stop: $scope.dropCallback,
          out: $scope.outCallback,
          over:  $scope.overCallback,
          disabled: !$scope.canDrag,
          revert: false
        };

        $scope.revertFunction = function(isValid) {
          if (isValid) return false;

          $scope.$apply(function () {
            if (_.isArray($scope.listTargets[$scope.targetIndex]))
              $scope.listTargets[$scope.targetIndex] = _.filter($scope.listTargets[$scope.targetIndex], function (el) {
                return el.id != $scope.dragging.id;
              });
            else
              $scope.listTargets[$scope.targetIndex] = {};

            for (var i = 0; i < $scope.listAnswers.length; i++) {
              if ($scope.originalListAnswers[i].id == $scope.dragging.id)
                $scope.listAnswers[i] = $scope.originalListAnswers[i];
            }

            $scope.initMathML(0);
          });

          return true;
        };

        $scope.dropOptions = {
          accept: function() {
            return $scope.dragging.fromTarget != attrs.identifier;
          },
          hoverClass: 'drop-hover'
        };

        $scope.draggableOptions = {
          revert: $scope.revertFunction
        };

        $scope.dropCallback = function (event, ui) {
          $scope.$parent.dropCallback(event, ui);
          $scope.dragging.isOut = false;
          setTimeout(function () {
            $(el).find('.contentElement').width($scope.maxWidth);
            $(el).find('.contentElement').height($scope.maxHeight);
          });
        };

        $scope.$on('unsetSelection', function (event) {
          $scope.correctClass = "";
        });

        $scope.$watch("maxWidth + maxHeight", function () {
          var padding = 25;
          $scope.width = isMultiple ? ($scope.maxWidth + padding) * columnsPerRow : $scope.maxWidth + padding;
          $scope.lpHeight = $scope.maxHeight + 20;
          if (!_.isEmpty($scope.label))
            $scope.lpHeight += 15;
        });

        $scope.startCallback = function (ev, b) {
          $scope.dragging.id = $(ev.target).attr('data-id');
          $scope.dragging.draggingFromAnswer = false;
        };

        $scope.$watch("canDrag", function(newVal) {
          $scope.sortableOptions.disabled = !newVal;
        });

        $scope.$watch("listTargets[" + $scope.targetIndex + "]", function () {
          $scope.itemContent = $scope.listTargets[$scope.targetIndex];
          $scope.itemContent.title = $scope.contentMap[$scope.listTargets[$scope.targetIndex].id];
        });

        $scope.$watch('itemSession.sessionData.correctResponses', function (responses) {
          if (!responses) return;
          if (!$scope.highlightUserResponse()) return;
          var correctResponse = QtiUtils.getResponseValue($scope.responseIdentifier, responses, []);
          var correctResponseForTarget = _.find(correctResponse,function (elem) {
            var s1 = elem.split(":")[0];
            return s1 == attrs.identifier;
          }).split(":")[1].split(",");
          var ourResponse = QtiUtils.getResponseValue($scope.responseIdentifier, $scope.itemSession.responses, []);
          var ourResponseForTarget = _.find(ourResponse, function (elem) {
            var s1 = elem.split(":")[0];
            return s1 == attrs.identifier;
          });
          ourResponseForTarget = ourResponseForTarget ? ourResponseForTarget.split(":")[1].split("|") : "";
          var isCorrect = $scope.isOrdered ? QtiUtils.compareArrays(correctResponseForTarget, ourResponseForTarget) : QtiUtils.compareArraysIgnoringOrder(correctResponseForTarget, ourResponseForTarget);
          $scope.correctClass = isCorrect ? "correct" : "incorrect";
          _.defer(function () {
            $(el).find('.contentElement').width($scope.maxWidth);
            $(el).find('.contentElement').height($scope.maxHeight);
          });
        });
      }
    }
  }
});


// Directive for showing the solution in a landingPlace (these are replaced by landingSolutions in the
// solution popup)
angular.module('qti.directives').directive("landingsolution", function (QtiUtils) {
  return {
    restrict: 'E',
    require: "^draganddropinteraction",
    replace: true,
    scope: true,
    compile: function (el, attrs) {
      var template =
        [
          '<ul style="min-height: {{maxHeight}}px; min-width: {{width}}px" class="thumbnail {{correctClass}} landing '+attrs['class']+'">',
          ' <div class="landingLabelHolder" ng-show="label">',
          '  <span class="landingLabel" style="">{{label}}</span>',
          ' </div>',
          ' <li ng-repeat="item in items" class="contentElement" ng-bind-html-unsafe="item" />',
          ' <div class="clearfix"></div>',
          '</ul>'
        ].join(" ");

      el.html(template);

      return function ($scope, el, attrs) {
        $scope.isMultiple = attrs.cardinality == 'multiple' || attrs.cardinality == 'ordered';
        $scope.items = [];

        $scope.label = attrs.label;

        $scope.$watch("maxWidth", function () {
          $scope.width = $scope.isMultiple ? (4 * $scope.maxWidth) : $scope.maxWidth;
        });

        $scope.$watch('itemSession.sessionData.correctResponses', function (responses) {
          if (!responses) return;
          var correctResponse = QtiUtils.getResponseValue($scope.responseIdentifier, responses, []);
          var correctResponseForTarget = _.find(correctResponse,function (elem) {
            var s1 = elem.split(":")[0];
            return s1 == attrs.identifier;
          }).split(":")[1].split(",");
          $scope.items = _.map(correctResponseForTarget, function (e) {
            return $scope.contentMap[e];
          });
        });
      }
    }
  }
});

