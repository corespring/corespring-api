angular.module('qti.directives').directive('dimensionsChecker', function() {

  return {

    link: function($scope, $element) {

      var $body = $element.find('body');

      var lastW = null;
      var lastH = null;

      function different(w, h) {
        if (!lastW || !lastH) {
          return true;
        }
        return lastW !== w || lastH !== h;
      }

      function getParent() {
        return (parent && parent != window) ? parent : null;
      }

      function postMessage(msg) {
        if (getParent() && getParent().postMessage) {
          getParent().postMessage(JSON.stringify(msg), "*");
          return true;
        }
        return false;
      }

      function dimensionsUpdate(w, h) {
        return {
          message: 'dimensionsUpdate',
          w: w,
          h: h
        }
      }

      function dispatchDimensions() {
        var b = $body[0];
        if (!b) return;

        var w = b.clientWidth;
        var h = b.clientHeight;

        if (different(w, h) && postMessage(dimensionsUpdate(w, h))) {
          lastW = w;
          lastH = h;
        }
      }

      setInterval(dispatchDimensions, 400);
      dispatchDimensions();
    }
  };
});
