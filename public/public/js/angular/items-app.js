var app = angular.module('app', ['itemResource', 'fieldValuesResource', 'tagger.services', 'preview.services', 'ui', 'corespring-utils']);


angular.module('app')
  .directive('iframeAutoHeight', function () {
    return {
      link: function ($scope, element) {
        $(element).load(function() {
          var $body = $(element, window.top.document).contents().find('body');
          var prevHeight = 0;
          setInterval(function() {
            try {
              var newHeight = $body[0].scrollHeight;
              if (newHeight == 0) return;
              if (newHeight != prevHeight) {
                $(element).height(newHeight);
                prevHeight = newHeight;
              }
            } catch (ie) {
              console.log(ie);
            }
          }, 100);
        });
      }
    }
  });

angular.module('app')
  .directive('showWhenLoaded', function () {
    return {
      link: function ($scope, element) {
        $(element).load(function () {
          if ($scope.itemData)
            $(element).show();
        })
      }
    }
  });

angular.module('app')
  .directive('hideWhenLoaded', function () {
    return {
      link: function ($scope, element, attrs) {
        $(element).load(function () {
          if ($scope.itemData)
            $(attrs.hideWhenLoaded).hide();
        })
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
      $('#itemViewFrame').hide();
      $('#preloader').show();
    }, 50);
    $timeout(function () {
      $('.window-overlay').scrollTop(0);
    }, 100);

  };

}
ItemsCtrl.$inject = ['$scope', '$timeout'];
