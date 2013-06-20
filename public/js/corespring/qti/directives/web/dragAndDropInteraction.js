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
      $scope.assignments = {};
      $scope.dragging = {};
      $scope.canDrag = true;

      $scope.resetClick = function() {
        for (var i = 0; i < $scope.listTargets.length; i++) {
          $scope.listTargets[i] = {};
        }
        for (var i = 0; i < $scope.listAnswers.length; i++) {
          $scope.listAnswers[i] = $scope.originalListAnswers[i];
        }
        $scope.initMathML(0);
      }
    },

    compile: function(elem,attrs) {
      var originalHtml = elem.html();

      elem.html("<div><button ng-click='resetClick()' ng-show='canDrag'>Reset</button><br/>"+originalHtml+"</div>");
      return function ($scope, element, attrs, AssessmentItemCtrl) {

        $scope.dropCallback = function(event, ui) {
          $scope.initMathML(0);
        };

        var itemsCombinedValue = function () {
          return _.reduce($scope.listTargets, function (acc, el) {
            return acc + el.id + ",";
          }, "");
        };

        $scope.$watch("formSubmitted", function (newValue) {
          if (newValue != undefined) $scope.canDrag = !newValue;
        });

        $scope.$watch(function watchBothItems() {
          return itemsCombinedValue();
        }, function whenItemsChange() {
          var response = [];
          for (var target in $scope.targetMap) {
            var answer = $scope.listTargets[$scope.targetMap[target]].id;
            if (answer) response.push(answer + ":" + target);
          }
          AssessmentItemCtrl.setResponse($scope.responseIdentifier, response);
        });

        $scope.$on('highlightUserResponses', function () {
          console.log("highlighting user response");
          var value = QtiUtils.getResponseValue($scope.responseIdentifier, $scope.itemSession.responses, "");
          _.each(value, function (v) {
            var arr = v.split(":");
            $scope.listTargets[$scope.targetMap[arr[1]]] = {id: arr[0]};
            $scope.listAnswers = _.map($scope.listAnswers, function (a) {
              if (a.id == arr[0]) return {}; else return a;
            });
          });
        });
      }
    }

  }
});

angular.module('qti.directives').directive("draggableanswer", function (QtiUtils) {
  return {
    restrict: 'E',
    require: "^draganddropinteraction",
    replace: true,
    scope: true,
    compile: function (tElement, tAttrs, transclude) {
      var originalContent = tElement.html();
      var template = [
        '<div class="answerContainer thumbnail {{correctClass}}" style="width: {{width}}; height: {{height}}" data-drop="true" ng-model="listAnswers" data-jqyoui-options="optionsList1" jqyoui-droppable="{index: {{$index}}, onDrop: \'dropCallback\'}">',
        ' <div class="btn btn-primary contentElement" ng-bind-html-unsafe="itemContent.title"',
        ' data-drag="{{canDrag}}" jqyoui-draggable="{index: {{$index}},placeholder:keep,animate:true,onStart:\'startCallback\'}"',
        ' data-jqyoui-options="{revert: \'invalid\'}" ng-model="listAnswers" ng-show="listAnswers[$index].id"></div>',
        '</div>'].join(" ");

      tElement.html(template);

      return function ($scope, el, attrs) {
        $scope.$index = $scope.indexes.answerIndex++;
        $scope.contentMap[attrs.identifier] = originalContent;
        $scope.listAnswers.push({id: attrs.identifier});
        $scope.originalListAnswers.push({id: attrs.identifier, title: originalContent});
        $scope.width = attrs.width ? attrs.width : "50px";
        $scope.height = attrs.height ? attrs.height : "50px";

        $scope.optionsList1 = {
          accept: function() {
            return !$scope.dragging.draggingFromAnswer || $scope.dragging.id == attrs.identifier;
          }
        };

        $scope.startCallback = function() {
          $scope.dragging.id = $scope.listAnswers[$scope.$index].id;
          $scope.dragging.draggingFromAnswer = true;
        }

        $scope.$watch("listAnswers[" + $scope.$index + "]", function () {
          $scope.itemContent = $scope.listAnswers[$scope.$index];
          $scope.itemContent.title = $scope.contentMap[$scope.listAnswers[$scope.$index].id];
        });

        $scope.$watch('itemSession.sessionData.correctResponses', function (responses) {
          if (!responses) return;
          var correctResponse = QtiUtils.getResponseValue($scope.responseIdentifier, responses, []);
          var correctTargetForAnswer = _.find(correctResponse, function (elem) {
            var s1 = elem.split(":")[0];
            return s1 == attrs.identifier;
          });
          correctTargetForAnswer = correctTargetForAnswer ? correctTargetForAnswer.split(":")[1] : undefined;
          var ourResponse = QtiUtils.getResponseValue($scope.responseIdentifier, $scope.itemSession.responses, []);
          var ourTargetForAnswer = _.find(ourResponse, function (elem) {
            var s1 = elem.split(":")[0];
            return s1 == attrs.identifier;
          });
          ourTargetForAnswer = ourTargetForAnswer ? ourTargetForAnswer.split(":")[1] : undefined;
          $scope.correctClass = correctTargetForAnswer == ourTargetForAnswer ? "correct" : "incorrect";
        });

      }
    }
  }
});

angular.module('qti.directives').directive("dragtarget", function (QtiUtils) {
  return {
    restrict: 'E',
    require: "^draganddropinteraction",
    template: [
      '<div style="height: 50px; width: 50px" class="thumbnail {{correctClass}}" data-drop="true" ng-model="listTargets" jqyoui-droppable="{index: {{$index2}}, onDrop: \'dropCallback\'}" data-jqyoui-options="optionsList2">',
      ' <div class="btn btn-primary"',
      ' data-drag="{{canDrag}}" jqyoui-draggable="{index: {{$index2}}, placeholder:keep, animate:true, onStart: \'startCallback\'}"',
      ' data-jqyoui-options="{revert: \'invalid\'}" ng-model="listTargets" ng-show="itemContent.title" ng-bind-html-unsafe="itemContent.title"></div>',
      '</div>'].join(" "),
    replace: true,
    scope: true,
    link: function ($scope, el, attrs) {
      $scope.$index2 = $scope.indexes.targetIndex++;
      $scope.listTargets.push({});
      $scope.targetMap[attrs.identifier] = $scope.$index2;

      $scope.optionsList2 = {
        accept: function() {
          return true;
          return !$scope.dragging.draggingFromAnswer || !$scope.listTargets[$scope.$index2].id;
        }
      };

      $scope.startCallback = function() {
        $scope.dragging.id = $scope.listTargets[$scope.$index2].id;
        $scope.dragging.draggingFromAnswer = false;
      }

      $scope.$watch("listTargets[" + $scope.$index2 + "]", function () {
        $scope.itemContent = $scope.listTargets[$scope.$index2];
        $scope.itemContent.title = $scope.contentMap[$scope.listTargets[$scope.$index2].id];
      });

      $scope.$watch('itemSession.sessionData.correctResponses', function (responses) {
        if (!responses) return;
        var correctResponse = QtiUtils.getResponseValue($scope.responseIdentifier, responses, []);
        var correctResponseForTarget = _.find(correctResponse,function (elem) {
          var s1 = elem.split(":")[1];
          return s1 == attrs.identifier;
        }).split(":")[0];
        var ourResponse = QtiUtils.getResponseValue($scope.responseIdentifier, $scope.itemSession.responses, []);
        var ourResponseForTarget = _.find(ourResponse,function (elem) {
          var s1 = elem.split(":")[1];
          return s1 == attrs.identifier;
        });
        ourResponseForTarget = ourResponseForTarget ? ourResponseForTarget.split(":")[0] : "";
        $scope.correctClass = correctResponseForTarget == ourResponseForTarget ? "correct" : "incorrect";
      });

    }
  }
});


