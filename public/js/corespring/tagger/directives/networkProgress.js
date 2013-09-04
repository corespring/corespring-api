// module for showing search count and paging through search results in editor
angular.module('tagger')
  .directive('networkProgress', function () {

    return {
      link: function ($scope) {
        $scope.isLoading = false;

        $scope.$on('onNetworkLoading', function () {
          $scope.isLoading = true;
        });

        $scope.$on('onNetworkComplete', function () {
          $scope.isLoading = false;
        });

      }
    }
  });