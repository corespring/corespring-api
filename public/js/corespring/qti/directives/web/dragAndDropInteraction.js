angular.module('qti.directives').directive("draganddropinteraction", function () {
  return {
    restrict: 'E',
    controller: function ($scope) {
      $scope.indexes = {answerIndex: 0, targetIndex: 0};
      $scope.listAnswers = [];
      $scope.listTargets = [];
    }
  }
});

angular.module('qti.directives').directive("draggableanswer", function () {
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
        'data-jqyoui-options="{revert: \'invalid\'}" ng-model="listAnswers" ng-show="listAnswers[$index].title"></div>',
        '</div>'].join(" ");

      tElement.html(template);

      return function ($scope, el, attrs, ctrl, $timeout) {
        $scope.$index = $scope.indexes.answerIndex++;
        $scope.listAnswers.push({title: originalContent});
        $scope.$watch("listAnswers[" + $scope.$index + "]", function () {
          $scope.itemContent = $scope.listAnswers[$scope.$index];
        });
      }
    }
  }
});

angular.module('qti.directives').directive("dragtarget", function () {
  return {
    restrict: 'E',
    require: "^draganddropinteraction",
    template: [
      '<div style="height: 50px; width: 50px" class="thumbnail" data-drop="true" ng-model="listTargets" jqyoui-droppable="{index: {{$index2}}}">',
      '<div class="btn btn-primary"',
      'data-drag="true" jqyoui-draggable="{index: {{$index2}},placeholder:true,animate:true}"',
      'data-jqyoui-options="{revert: \'invalid\'}" ng-model="listTargets" ng-show="itemContent.title" ng-bind-html-unsafe="itemContent.title"></div>',
      '</div>'].join(" "),
    replace: true,
    scope: true,
    link: function ($scope, el, attrs, ctrl, $timeout) {
      $scope.$index2 = $scope.indexes.targetIndex++;
      $scope.listTargets.push({});
      $scope.$watch("listTargets[" + $scope.$index2 + "]", function () {
        $scope.itemContent = $scope.listTargets[$scope.$index2];
      });
    }
  }
});


