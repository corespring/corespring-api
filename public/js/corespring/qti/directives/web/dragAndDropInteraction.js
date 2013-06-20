angular.module('qti.directives').directive("draganddropinteraction", function (QtiUtils) {
  return {
    restrict: 'E',
    require: '^assessmentitem',
    scope: true,
    controller: function ($scope,$attrs) {
      $scope.responseIdentifier = $attrs.responseidentifier;
      $scope.indexes = {answerIndex: 0, targetIndex: 0};
      $scope.listAnswers = [];
      $scope.listTargets = [];
      $scope.targetMap = {};
      $scope.contentMap = {};
      $scope.assignments = {};
    },

    link: function($scope, element, attrs,AssessmentItemCtrl) {

      var itemsCombinedValue = function() {
        return _.reduce($scope.listTargets, function(acc,el) {
          return acc + el.id + ",";
        }, "");
      };

      $scope.$watch(function watchBothItems() {
        return itemsCombinedValue();
      }, function whenItemsChange() {
        var response = [];
        for (var target in $scope.targetMap) {
          var answer = $scope.listTargets[$scope.targetMap[target]].id;
          if (answer) response.push(answer+":"+target);
        }
        AssessmentItemCtrl.setResponse($scope.responseIdentifier, response);
        console.log(response);

      });

      $scope.$on('highlightUserResponses', function () {
        console.log("highlighting user response");
        var value = QtiUtils.getResponseValue($scope.responseIdentifier, $scope.itemSession.responses, "");
        console.log(value);
        _.each(value, function(v) {
          var arr = v.split(":");
          $scope.listTargets[$scope.targetMap[arr[1]]] = {id: arr[0]};
          $scope.listAnswers = _.map($scope.listAnswers, function(a) {
            if (a.id == arr[0]) return {}; else return a;
          });
        });
      });
    }
  }
});

angular.module('qti.directives').directive("draggableanswer", function (QtiUtils) {
  return {
    restrict: 'E',
    require: "^draganddropinteraction",
    replace: true,
    scope: true,
    compile: function(tElement, tAttrs, transclude) {
      var originalContent = tElement.html();
      var template =[
        '<div class="thumbnail" style="width: 50px; height: 50px" data-drop="true" ng-model="listAnswers" jqyoui-droppable="{index: {{$index}}}">',
        '<div class="btn btn-primary contentElement" ng-bind-html-unsafe="itemContent.title"',
        'data-drag="true" jqyoui-draggable="{index: {{$index}},placeholder:true,animate:true}"',
        'data-jqyoui-options="{revert: \'invalid\'}" ng-model="listAnswers" ng-show="listAnswers[$index].id"></div>',
        '</div>'].join(" ");

      tElement.html(template);

      return function ($scope,el,attrs) {
        $scope.$index = $scope.indexes.answerIndex++;
        $scope.contentMap[attrs.identifier] = originalContent;
        $scope.listAnswers.push({id: attrs.identifier});
        $scope.$watch("listAnswers[" + $scope.$index + "]", function () {
          $scope.itemContent = $scope.listAnswers[$scope.$index];
          $scope.itemContent.title = $scope.contentMap[$scope.listAnswers[$scope.$index].id];
        });

        $scope.$watch('itemSession.sessionData.correctResponses', function (responses) {
          if (!responses) return;
          var correctResponse = QtiUtils.getResponseValue($scope.responseIdentifier, responses, []);
          var ourResponse = QtiUtils.getResponseValue($scope.responseIdentifier, $scope.itemSession.responses, []);
//          console.log("Correct: "+correctResponse);
//          console.log("Ours: "+ourResponse);
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
      '<div style="height: 50px; width: 50px" class="thumbnail {{correctClass}}" data-drop="true" ng-model="listTargets" jqyoui-droppable="{index: {{$index2}}}">',
      '<div class="btn btn-primary"',
      'data-drag="true" jqyoui-draggable="{index: {{$index2}},placeholder:true,animate:true}"',
      'data-jqyoui-options="{revert: \'invalid\'}" ng-model="listTargets" ng-show="itemContent.title" ng-bind-html-unsafe="itemContent.title"></div>',
      '</div>'].join(" "),
    replace: true,
    scope: true,
    link: function ($scope,el,attrs) {
      $scope.$index2 = $scope.indexes.targetIndex++;
      $scope.listTargets.push({});
      $scope.targetMap[attrs.identifier] = $scope.$index2;
      $scope.$watch("listTargets[" + $scope.$index2 + "]", function () {
        $scope.itemContent = $scope.listTargets[$scope.$index2];
        $scope.itemContent.title = $scope.contentMap[$scope.listTargets[$scope.$index2].id];
      });

      $scope.$watch('itemSession.sessionData.correctResponses', function (responses) {
        if (!responses) return;
        var correctResponse = QtiUtils.getResponseValue($scope.responseIdentifier, responses, []);
        var correctResponseForTarget = _.find(correctResponse, function(elem) {
          var s1 = elem.split(":")[1];
          return s1 == attrs.identifier;
        }).split(":")[0];
        var ourResponse = QtiUtils.getResponseValue($scope.responseIdentifier, $scope.itemSession.responses, []);
        var ourResponseForTarget = _.find(ourResponse, function(elem) {
          var s1 = elem.split(":")[1];
          return s1 == attrs.identifier;
        }).split(":")[0];
        console.log("Correct: "+correctResponseForTarget);
        console.log("Ours: "+ourResponseForTarget);
        $scope.correctClass = correctResponseForTarget == ourResponseForTarget ? "correct" : "incorrect";
      });

    }
  }
});


