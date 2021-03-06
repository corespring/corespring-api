
/**
 * Service for page to frame communication using the postMessage api.
 */
angular.module('corespring-services', []).factory('MessageBridge', [function () {

  return {

    /**
     * Add handler for 'message'
     * @param fn
     */


    addMessageListener: function (fn) {
      var eventMethod = window.addEventListener ? "addEventListener" : "attachEvent";
      var eventer = window[eventMethod];
      var messageEvent = eventMethod == "attachEvent" ? "onmessage" : "message";

      eventer(messageEvent,function(e) {
        //console.log('parent received message!:  ',e.data);
        fn(e);
      },false);
    },

    /**
     * @param id - target either an iframe id or 'parent'
     * @param msg - a string or object - gets JSON.stringified unless disableStringify is true
     * @param disableStringify - Boolean, if true msg won't be JSON.stringified (please note
     * that IE only supports sending strings as message)
     */
    sendMessage: function (id, msg, disableStringify) {

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
        target.postMessage(disableStringify ? msg : JSON.stringify(msg), "*");
      }
    }
  }
}]);


