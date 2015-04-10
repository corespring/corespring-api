angular.module('tagger')
  .directive('profilePlayer', ['$timeout',function ($timeout) {

  var definition = {
    replace: false,
    restrict: 'A',
    template: '<div style=""><span class="close-window-button" ng-click="hidePopup()" style="z-index: 10"></span><div id="content"></div></div>',
    scope: {itemId: '@itemId', onItemLoad: '&onItemLoad'},
    link: function (scope, element, attrs) {
      scope.hidePopup = function() {
        scope.$parent.hidePopup();
      };

      scope.$watch("itemId", function (val) {
        if (!val) return;

        var options = {
          itemId: attrs.itemId,
          mode: "preview",
          width: "740px",
          autoHeight: true
        };

        var onError = function (err) {
          throw "Error loading test player: " + err.msg;
        };

        var onLoad = function () {
          scope.onItemLoad();
        };

        new org.corespring.players.ItemCatalog(
          angular.element(element).find('#content')[0],
          options, 
          function(error){
            console.error("error creating catalog " + error);
          });

        //0.24 of the container catalog doesn't have an onLoad callback,
        //timeout for now.
        $timeout(function(){
          scope.onItemLoad();
        }, 1000);
      });
    }
  };
  return definition;

}]);