angular.module('tagger')
  .directive('profilePlayer', ['CmsService',function (CmsService) {

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
          width: "640px",
          autoHeight: true
        };

        var onError = function (err) {
          throw "Error loading test player: " + err.msg;
        };


        var onLoad = function () {
          scope.onItemLoad();
        };

        CmsService.itemFormat(attrs.itemId, function(format){

          if(format.apiVersion === 1){
            new com.corespring.players.ItemProfile(
              angular.element(element).find('#content')[0], options, onError, onLoad);
          } else {
            new org.corespring.players.ItemCatalog(
              angular.element(element).find('#content')[0],
              options, 
              function(error){
                console.error("error creating catalog " + error);
              });
          }
        });

      });
    }
  };
  return definition;

}]);