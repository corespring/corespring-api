angular.module('tagger')
  .directive('sortColumnHeader', function () {

    return {
      template: ["<span class='sortableHeader' ng-class='{sortingHeader: isSortingOnThisField, descending: isDescending}' ng-click='click()'>",
                 "<span ng-transclude></span></span>"
                ].join(""),
      transclude: true,
      scope: true,
      link: function ($scope, element, attrs) {
        $scope.field = attrs.field;
        $scope.isMe = false;
        $scope.click = function() {
          $scope.sortBy($scope.field);
        }
        $scope.$on('sortingOnField', function (sender, field, isAscending) {
          $scope.isSortingOnThisField = field == $scope.field;
          $scope.isDescending = !isAscending;
        });
      }
    }
  });