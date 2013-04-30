var app = angular.module('app', ['itemResource', 'fieldValuesResource', 'tagger.services', 'preview.services', 'ui', 'corespring-utils']);


angular.module('app').directive('profilePlayer', function () {

  var definition = {
    replace: false,
    restrict: 'A',
    template: '<div style=""><span class="close-window-button" ng-click="hidePopup()" style="z-index: 10"></span><div id="content"></div></div>',
    scope: {itemId: '@itemId', onItemLoad: '&onItemLoad'},
    link: function (scope, element, attrs) {
      scope.hidePopup = function() {
        scope.$parent.hidePopup();
      }

      scope.$watch("itemId", function (val) {
        if (!val) return;
        var options = {
          itemId: attrs.itemId,
          mode: "preview",
          width: "640px",
          autoHeight: true
        };

        var onError = function (err) {
          throw "Error loading test player: " + err.msg;
        };


        var onLoad = function () {
          scope.onItemLoad();
        };

        new com.corespring.players.ItemProfile(angular.element(element).find('#content')[0], options, onError, onLoad);

      });
    }
  };
  return definition;

});


function ItemsCtrl($scope, $timeout) {

  $scope.hidePopup = function() {
    $scope.showPopup = false;
    $scope.previewingId = "";
  }

  $scope.openItem = function (id) {
    $timeout(function () {
      $scope.showPopup = true;
      $scope.previewingId = id;
      $scope.$broadcast("requestLoadItem", id);
      $('#preloader').show();
      $('#player').hide();
    }, 50);
    $timeout(function () {
      $('.window-overlay').scrollTop(0);
    }, 100);

  };


  $scope.onItemLoad = function () {
    $('#preloader').hide();
    $('#player').show();
  }

  var fn = function(m) {
    var data = JSON.parse(m.data);
    if (data.message == 'closeProfilePopup') {
      $timeout(function() {
        $scope.hidePopup();
      }, 10);
    }
  }

  if (window.addEventListener) {
    window.addEventListener('message', fn, true);
  }
  else if (window.attachEvent) {
    window.attachEvent('message', fn);
  }

}
ItemsCtrl.$inject = ['$scope', '$timeout'];
