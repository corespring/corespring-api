angular.module('corespring-services', []).factory('MessageBridge', [function () {

  return {

    addMessageListener: function (fn) {
      if (window.addEventListener) {
        window.addEventListener('message', fn, true);
      }
      else if (window.attachEvent) {
        window.attachEvent('message', fn);
      } else {
        throw "couldn't add message listener";
      }
    },

    sendMessage: function (id, msg) {

      function getParent(){ return (parent && parent != window) ? parent : null; }
      function getIframe(id){
        try{
          return document.getElementById(id).contentWindow;
        }
        catch(e){
          return null;
        }
      }
      var target = (!id || id === "parent" ) ? getParent() : getIframe(id);

      if(target){
        target.postMessage(JSON.stringify(msg), "*");
      }
    }
  }
}]);


