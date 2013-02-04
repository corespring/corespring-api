var app = angular.module('app', ['itemResource', 'fieldValuesResource', 'tagger.services', 'preview.services', 'ui', 'corespring-utils']);


angular.module('app')
  .directive('iframeAutoHeight', function () {
    return {
      link: function ($scope, element) {
        $(element).iframeAutoHeight({});
      }
    }
  });

function ItemsCtrl($scope, $timeout) {

  $scope.hidePopup = function () {
    $scope.showPopup = false;
  };

  $scope.openItem = function (id) {
    $timeout(function () {
      $scope.showPopup = true;
      $scope.previewingId = id;
      $scope.$broadcast("requestLoadItem", id);
      $('#itemViewFrame').height("600px");
    }, 50);
    $timeout(function () {
      $('.window-overlay').scrollTop(0);
    }, 100);

  };

}
ItemsCtrl.$inject = ['$scope', '$timeout'];
