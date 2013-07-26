/** This player just shows an error in the target element and logs the error via the errorCallback
 */
(function (root) {
    var com = root.com = root.com || {};

    com.corespring = com.corespring || {};
    com.corespring.players = {};

    com.corespring.players.mainError = "${playerError}";
    com.corespring.players.errors = {};

    com.corespring.players.ItemProfile = function (element, options, errorCallback, onLoadCallback) {
        return new com.corespring.players.ItemPlayer(element, options, errorCallback);
    };

    com.corespring.players.ItemPlayer = function (element, options, errorCallback) {

        var codes = com.corespring.players.errors;

        var error = function (msg, code) {
            if(errorCallback) {
                errorCallback({msg: msg, code: code});
            } else if(console && console.error) {
               console.error(msg, code);
            }
        };

        if (!jQuery) {
            error("jQuery not found", codes.NEED_JQUERY);
        }

        var e = $(element);

        if (!e) {
            error("Container element not found.", codes.NEED_EMBEDDING_ELEMENT);
            return;
        }

        if(e.length !== 1){
            error("Container element must be unique", codes.MULTIPLE_ELEMENTS);
            return;
        }
        e.append("<p>An Error has occured loading the player - please contact your system administrator");
        error(com.corespring.players.mainError);

    };
})(this);

