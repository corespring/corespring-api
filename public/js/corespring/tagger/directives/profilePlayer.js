angular.module('tagger').directive('profilePlayer', function () {

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