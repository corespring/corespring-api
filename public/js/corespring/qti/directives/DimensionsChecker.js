angular.module('qti.directives').directive('dimensionsChecker', function(){

  return {

    link: function($scope, $element){

      var $body = $element.find('body');

      var lastW = null;
      var lastH = null;

      var different = function(w,h){
        if(!lastW || !lastH){
          return true;
        }
        return lastW !== w || lastH !== h;
      };

      var getParent = function(){ return (parent && parent != window) ? parent : null; };

      var dispatchDimensions = function(){
        var b = $body[0];
        var w = b.clientWidth;
        var h = b.clientHeight;

        if(different(w,h)){
          lastW = w;
          lastH = h;
          var msg = {message:'dimensionsUpdate', w: w, h: h};
          if (getParent()) getParent().postMessage(JSON.stringify(msg), "*");
        }
      };

      setInterval(dispatchDimensions, 400);
      dispatchDimensions();
    }
  };
});