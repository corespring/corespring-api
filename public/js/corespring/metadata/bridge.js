(function (root) {
    var com = root.com = root.com || {};

    com.corespring = com.corespring || {};
    com.corespring.metadataBridge = {};

    var sendMessage = function (type, message) {
        window.top.postMessage({type: type, message: message}, "*");
    };

    var addMessageListener = function (fn) {
        var eventMethod = window.addEventListener ? "addEventListener" : "attachEvent";
        var eventer = window[eventMethod];
        var messageEvent = eventMethod == "attachEvent" ? "onmessage" : "message";

        eventer(messageEvent, function (e) {
            fn(e);
        }, false);
    };

    com.corespring.metadataBridge.requestMetadata = function(callback) {
        addMessageListener(function (msg) {
            if (msg.data.type == "currentMetadata") {
                callback(msg.data.message);
            }
        });

        sendMessage('requestMetadata', "");
    };

    com.corespring.metadataBridge.updateMetadata = function(metadata) {
        sendMessage('updateMetadata', metadata);
    };

})(this);