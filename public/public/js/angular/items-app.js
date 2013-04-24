var app = angular.module('app', ['itemResource', 'fieldValuesResource', 'tagger.services', 'preview.services', 'ui', 'corespring-utils']);


angular.module('app')
  .directive('iframeAutoHeight', function () {
    return {
      link: function ($scope, element) {
        $(element).load(function () {
          var $body = $(element, window.top.document).contents().find('body');
          var prevHeight = 0;
          setInterval(function () {
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
          $(element).show();
        });
      }
    }
  });

angular.module('app')
  .directive('hideWhenLoaded', function () {
    return {
      link: function ($scope, element, attrs) {
        $(element).load(function () {
          $(attrs.hideWhenLoaded).hide();
        })
      }
    }
  });


angular.module('app').directive('profilePlayer', function () {

  var definition = {
    replace: true,
    restrict: 'ACE',
    template: "<div class='iframe-600-centered'/>",
    scope: {itemId: '@itemId', onItemLoad: '&onItemLoad'},
    link: function (scope, element, attrs) {
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

        new com.corespring.players.ItemProfile(element, options, onError, onLoad);

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
      $('#itemViewFrame').height("600px");
      $('#itemViewFrame').hide();
      $('#preloader').show();
    }, 50);
    $timeout(function () {
      $('.window-overlay').scrollTop(0);
    }, 100);

  };


  $scope.onItemLoad = function () {
    $('#preloader').hide();
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
