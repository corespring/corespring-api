var app = angular.module('app', ['itemResource', 'fieldValuesResource', 'tagger.services', 'preview.services', 'ui', 'corespring-utils']);

angular.module('app')
  .directive('iframeAutoHeight', function () {
    return {
      link:function ($scope, element, attr, ctrl) {
        console.log("Iframe Auto Height");
        $(element).iframeAutoHeight({debug: true});
      }
    }
  });

function ItemsCtrl($scope, $timeout) {


    $scope.hidePopup = function() {
      console.log("Hiding Item");
      $scope.showPopup = false;
    };

    $scope.openItem = function(id) {
      $timeout(function() {
        console.log("Opening item");
        $scope.showPopup = true;
        $scope.previewingId = id;
        $scope.$broadcast("requestLoadItem", id);

      }, 50);
      $timeout(function() {
        $('.window-overlay').scrollTop(0);
      }, 100);

    };

}
ItemsCtrl.$inject = ['$scope', '$timeout'];
