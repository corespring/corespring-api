angular.module('qti.directives').directive('dimensionsChecker', function(){

  return {

    link: function($scope, $element){

      var $body = $element.find('body');

      var getParent = function(){ return (parent && parent != window) ? parent : null; }

      var dispatchDimensions = function(){
        var w = $body.width();
        var h = $body.height();
        var msg = {message:'dimensionsUpdate', w: w, h: h};
        getParent().postMessage(JSON.stringify(msg), "*");
      };

      setInterval(dispatchDimensions, 500);
      dispatchDimensions();
    }
  }
});