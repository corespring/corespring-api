angular.module('tagger')
  .directive('sortColumnHeader', function () {

    return {
      template: "<span ng-click='click()'><span ng-show='isSortingOnThisField'>*</span><span ng-transclude></span></span>",
      transclude: true,
      scope: true,
      link: function ($scope, element, attrs) {
        $scope.field = attrs.field;
        $scope.isMe = false;
        $scope.click = function() {
          $scope.sortBy($scope.field);
        }
        $scope.$on('sortingOnField', function (sender, field) {
          $scope.isSortingOnThisField = field == $scope.field;
        });
      }
    }
  });