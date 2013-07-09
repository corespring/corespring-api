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
      $scope.orderMatters = $attrs.ordermatters == "true";
      $scope.maxWidth = 50;
      $scope.maxHeight = 20;
      $scope.stateStack = [];

      $scope.getTargetIndex = function (target) {
        return $scope.targetMap[target];
      };

      $scope.getContentForId = function (id) {
        return $scope.contentMap[id];
      };

      $scope.propagateDimension = function (w, h) {
        if (w > $scope.maxWidth) $scope.maxWidth = w;
        if (h > $scope.maxHeight) $scope.maxHeight = h;
      };

      $scope.undo = function () {
        if ($scope.stateStack.length <= 1) return;
        $scope.stateStack.pop();
        var state = _.last($scope.stateStack);
        $scope.listAnswers = QtiUtils.deepCopy(state.answers);
        $scope.listTargets = QtiUtils.deepCopy(state.targets);
      }

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

      $scope.dropCallback = function (event, ui) {
        $scope.initMathML(0);

        //Remove empty objects from multiple (array based) targets
        _.each($scope.listTargets, function (el, idx) {
          if (_.isArray(el)) {
            var filtered = _.filter(el, function (el2) {
              return el2.id;
            });
            $scope.listTargets[idx] = filtered;
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
        return "<div ui-modal ng-model='solutionVisible' class='solution-modal'>" + solutionHtml + "</div>";
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

      var solutionButtonHtml = "<div class='button-row'><button class='btn solution-button' ng-click='solutionVisible = true' ng-hide='canDrag'>See solution</button></div>";

      elem.html(
        [
          getAnswerAreaTemplate(originalHtml),
          "<div>",
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
          $scope.correctString = $scope.itemSession.responses[0].outcome.isCorrect.toString();
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
        '<div class="answerContainer {{correctClass}} {{phClass}}" data-drop="true" ng-model="listAnswers" data-jqyoui-options="dropOptions" jqyoui-droppable="{index: {{$index}}, onDrop: \'dropCallback\'}">',
        ' <div class="contentElement" ng-bind-html-unsafe="itemContent.title"',
        ' data-drag="{{canDrag}}" jqyoui-draggable="{index: {{$index}},placeholder:' + placeHolder + ',animate:false,onStart:\'startCallback\',onStop:\'stopCallback\'}"',
        ' data-jqyoui-options="{revert: \'invalid\',helper: ' + helper + '}" ng-model="listAnswers" ng-show="listAnswers[$index].id"></div>',
        ' <div class="clearfix"></div>',
        '</div>',

        '<div class="sizerHolder" style="display: none; position: absolute">',
        originalContent,
        '</div>'].join(" ");

      tElement.html(template);

      return function ($scope, el, attrs) {
        $scope.$index = $scope.indexes.answerIndex++;
        $scope.contentMap[attrs.identifier] = originalContent;
        $scope.listAnswers.push({id: attrs.identifier});
        $scope.originalListAnswers.push({id: attrs.identifier, title: originalContent});
        $scope.width = attrs.width ? attrs.width : "50px";
        $scope.height = attrs.height ? attrs.height : "50px";
        $scope.copyOnDrag = attrs.copyondrag == "true";
        $scope.placeholderClass = attrs.placeholderClass;
        var lastW, lastH, sizeNotChangedCounter = 0;
        var interval = setInterval(function () {
          $scope.$apply(function () {

            var w = $(el).find('.sizerHolder').width();
            var h = $(el).find('.sizerHolder').height();

            if (lastW != w || lastH != h) {
              $scope.propagateDimension(w, h);
              sizeNotChangedCounter = 0;
            } else {
              sizeNotChangedCounter++;
            }

            if (sizeNotChangedCounter > 5) {
              console.log("Size has settled");
              clearInterval(interval);
            }
            lastW = w;
            lastH = h;
          });
        }, 1000);

        $scope.dropOptions = {
          accept: function () {
            return $scope.dragging.id == attrs.identifier;
          }
        };

        $scope.startCallback = function () {
          $scope.dragging.id = $scope.listAnswers[$scope.$index].id;
          $scope.dragging.draggingFromAnswer = true;

          $scope.$apply(function () {
            $scope.phClass = $scope.placeholderClass;
          });
        };

        $scope.stopCallback = function () {
          $scope.$apply(function () {
            $scope.phClass = "";
          });
        };

        $scope.$watch("listAnswers[" + $scope.$index + "]", function () {
          $scope.itemContent = $scope.listAnswers[$scope.$index];
          $scope.itemContent.title = $scope.contentMap[$scope.listAnswers[$scope.$index].id];
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
      var isMultiple = attrs.cardinality == 'multiple';

      var originalHtml = el.html();

      var template =
        [
          '<div style="min-height: {{maxHeight}}px; width: {{width}}px" class="landing {{correctClass}}" data-drop="true" ng-model="%model"',
          'jqyoui-droppable="{onDrop: \'dropCallback\', multiple: true}" data-jqyoui-options="{hoverClass: \'drop-hover\'}">',
          '<div class="landingLabelHolder" ng-show="label">',
          ' <span class="landingLabel" style="">{{label}}</span>',
          '</div>',
          ' <div %repeat class="contentElement"',
          ' data-drag="{{canDrag}}" jqyoui-draggable="{index: {{$index}}, placeholder:true, animate:false, onStart: \'startCallback\'}"',
          ' data-jqyoui-options="draggableOptions" ng-model="%model" ng-show="itemContent.title" ng-bind-html-unsafe="itemContent.title" data-id="{{itemContent.id}}"></div>',
          '<div class="clearfix"></div>',
          originalHtml,
          '</div>'].join("");

      if (isMultiple)
        template = template.replace(/%model/g, "listTargets[$index]").replace(/%repeat/g, "ng-repeat=\"itemContent in listTargets[$index]\"")
      else
        template = template.replace(/%model/g, "listTargets[$index]").replace(/%repeat/g, "");

      el.html(template);

      return function ($scope, el, attrs) {
        $scope.isMultiple = attrs.cardinality == 'multiple';
        var defaultWidth = $scope.isMultiple ? "200" : "50";
        $scope.width = defaultWidth;

        $scope.$index = $scope.indexes.targetIndex++;
        $scope.listTargets.push($scope.isMultiple ? [] : {});
        $scope.targetMap[attrs.identifier] = $scope.$index;
        $scope.label = attrs.label;

        $scope.draggableOptions = {
          revert: function (isValid) {
            if (isValid) return false;

            $scope.$apply(function () {
              if (_.isArray($scope.listTargets[$scope.$index]))
                $scope.listTargets[$scope.$index] = _.filter($scope.listTargets[$scope.$index], function (el) {
                  return el.id != $scope.dragging.id;
                });
              else
                $scope.listTargets[$scope.$index] = {};

              for (var i = 0; i < $scope.listAnswers.length; i++) {
                if ($scope.originalListAnswers[i].id == $scope.dragging.id)
                  $scope.listAnswers[i] = $scope.originalListAnswers[i];
              }

              $scope.initMathML(0);
            });

            return true;
          }
        }

        $scope.dropCallback = function (event, ui) {
          $scope.$parent.dropCallback(event, ui);
          setTimeout(function () {
            $(el).find('.contentElement').width($scope.maxWidth);
            $(el).find('.contentElement').height($scope.maxHeight);
          });
        }

        $scope.$watch("maxWidth + maxHeight", function () {
          $scope.width = isMultiple ? $scope.maxWidth * 4 : $scope.maxWidth + 20;
        });

        $scope.startCallback = function (ev, b) {
          $scope.dragging.id = $(ev.target).attr('data-id');
          $scope.dragging.draggingFromAnswer = false;
        }

        $scope.$watch("listTargets[" + $scope.$index + "]", function () {
          $scope.itemContent = $scope.listTargets[$scope.$index];
          $scope.itemContent.title = $scope.contentMap[$scope.listTargets[$scope.$index].id];
        });

        $scope.$watch('itemSession.sessionData.correctResponses', function (responses) {
          if (!responses) return;
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
          var isCorrect = $scope.orderMatters ? QtiUtils.compareArrays(correctResponseForTarget, ourResponseForTarget) : QtiUtils.compareArraysIgnoringOrder(correctResponseForTarget, ourResponseForTarget);
          $scope.correctClass = isCorrect ? "correct" : "incorrect";
          setTimeout(function () {
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
          '<div style="min-height: {{maxHeight}}px; min-width: {{width}}px" class="thumbnail {{correctClass}}">',
          ' <div ng-repeat="item in items" class="contentElement" ng-bind-html-unsafe="item"></div>',
          '<div class="clearfix"></div>',
          '</div>'].join(" ");

      el.html(template);

      return function ($scope, el, attrs) {
        $scope.isMultiple = attrs.cardinality == 'multiple';
        $scope.items = [];

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

