(function (root) {
  var console = window.console || {log: function(){}};
  var com = root.com = root.com || {};

  com.corespring = com.corespring || {};
  com.corespring.players = {};

  var rootListener = null;

  var eventName = function() { return window.addEventListener ? "message" : "onmessage";}
  var addEventFunctionName = function(){ return window.addEventListener ? "addEventListener" : "attachEvent"; }

  /** only add one listener to the window, this function is expected to then delegate out to player listeners */
  var addRootLevelMessageListener = function (newListener) {
    if (rootListener != null) {
      throw "A root level listener is already registered!";
    }
    rootListener = newListener;
    window[addEventFunctionName()](eventName(), rootListener, false);
  };

  /** The root listener implementation - forward event to all player listeners */
  var rootLevelListener = function (e) {
    for (var i = 0; i < playerListeners.length; i++) {
      playerListeners[i](e);
    }
  };

  addRootLevelMessageListener(rootLevelListener);

  /** A cache of existing player listeners - gets overrwritten when a new ItemPlayer is instantiated */
  var playerListeners = [];

  var addPlayerListener = function (fn) {
    if (playerListeners.indexOf(fn) == -1) {
      playerListeners.push(fn);
    }
  }

  var clearPlayerListeners = function () {
    playerListeners = [];
  }

  com.corespring.players.config = {
    baseUrl: "${baseUrl}",
    paths: {
      preview: "/item/:itemId/preview",
      render: "/session/:sessionId/render",
      administerItem: "/item/:itemId/administer",
      administerSession: "/session/:sessionId/administer",
      aggregate: "/aggregate/:assessmentId/:itemId/run",
      profile: "/item/:itemId/profile"
    },
    mode: "${mode}"
  };

  com.corespring.players.errors = {
    NEED_OPTIONS: 0,
    NEED_MODE: 1,
    NEED_ITEMID_OR_SESSIONID: 2,
    NEED_EMBEDDING_ELEMENT: 3,
    INVALID_PLAYER_LOCATION: 4,
    NEED_JQUERY: 5,
    MULTIPLE_ELEMENTS: 6
  };

  function addDimensionChangeListener(element) {

    var listenerFunction = function (data, event) {
      try {
        var json = JSON.parse(data);
        if (json.message == 'dimensionsUpdate') {
          var frames = document.getElementsByTagName('iframe');
          var found = false;
          for (var i = 0; i < frames.length; i++) {
            if (frames[i].contentWindow == event.source) {
              $(frames[i]).height(json.h + 30);
              found = true;
              break;
            }
          }
          if (!found) $(element).height(json.h + 30);
        }
      } catch (e) {

      }
    }

    addPlayerListener(function(e){ listenerFunction(e.data, e) });
  }

  var isValidMode = function (m) {
    if (!m) return false;

    return ["preview", "administer", "render", "aggregate"].indexOf(m) !== -1;
  };

  var iframePlayerStrategy = function (e, options) {
    var url = options.corespringUrl;
    if (options.omitSubmitButton)
      url += "?omitSubmitButton=true";

    e.html("<iframe id='iframe-player' src='" + url + "' style='width: 100%; height: 100%; border: none'></iframe>");
    e.width(options.width ? options.width : "600px");


    if (options.height)
      e.height(options.height);
    else
      addDimensionChangeListener(e);

  };

  com.corespring.players.ItemProfile = function (element, options, errorCallback, onLoadCallback) {
    var base = com.corespring.players.config.baseUrl;
    options.corespringUrl = base + com.corespring.players.config.paths.profile.replace(":itemId", options.itemId);
    var e = $(element);
    iframePlayerStrategy(e, options);
    e.find('iframe').load(onLoadCallback);
  };

  /**
   * Construct a new ItemPlayer
   * @param element - a jquery style identifier of a single element (eg: #my-div)
   * @param options - the options object
   *    - At a minimum you must specify a mode and the relevant ids for that mode:
   *      - preview: itemId
   *      - render: sessionId
   *      - administer: itemId or sessionId
   *      - aggregate: assessmentId and itemId
   *
   * @param errorCallback - a function that handles any player errors. The error object has the form: {code: _, msg: _}
   */
  com.corespring.players.ItemPlayer = function (element, options, errorCallback) {

    clearPlayerListeners();

    var addSessionListener = function (message, callback, dataHandler) {

      dataHandler = (dataHandler || function (s) {
        return s.id;
      });

      addPlayerListener(function (event) {
        try {
          var dataString = event.data;
          var data = JSON.parse(dataString);
          if (data.message == message && data.session) {
            callback(dataHandler(data.session));
          }
        }
        catch (e) {

        }
      });
    };

    var codes = com.corespring.players.errors;

    var error = function (msg, code) {
      if (errorCallback) {
        errorCallback({msg: msg, code: code});
      } else if (console && console.error) {
        console.error(msg, code);
      } else {
        alert(msg);
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

    if (e.length !== 1) {
      error("Container element must be unique", codes.MULTIPLE_ELEMENTS);
      return;
    }

    if (!options) {
      error("Need to specify options", codes.NEED_OPTIONS);
      return;
    }

    if (!isValidMode(options.mode)) {
      error("Need a valid launch mode", codes.NEED_MODE);
      return;
    }

    if (options.onItemSessionCreated && options.mode === "administer") {
      addSessionListener("itemSessionCreated", options.onItemSessionCreated);
    }

    if (options.onItemSessionRetrieved && options.mode === "administer") {
      addSessionListener("itemSessionRetrieved", options.onItemSessionRetrieved);
    }

    if (options.onItemSessionCompleted && options.mode === "administer") {
      addSessionListener("sessionCompleted", options.onItemSessionCompleted);
    }

    var getUrl = function (mode, options) {
      var template = com.corespring.players.config.baseUrl;
      switch (options.mode) {
        case 'render':
          template += com.corespring.players.config.paths.render;
          break;
        case 'preview':
          template += com.corespring.players.config.paths.preview;
          break;
        case 'administer':
          if (options.itemId) template += com.corespring.players.config.paths.administerItem;
          else if (options.sessionId) template += com.corespring.players.config.paths.administerSession;
          break;
        case 'aggregate':
          template += com.corespring.players.config.paths.aggregate;
          break;
        default :
          error("Unknown mode", codes.NEED_MODE);
          break;
      }
      template = template
        .replace(":itemId", options.itemId)
        .replace(":sessionId", options.sessionId)
        .replace(":assessmentId", options.assessmentId);
      if(options.role) return addQueryString(template,options.role)
      else return template
    };

    function addQueryString(url,param){
        if(url.indexOf("?") != -1){
            return url+"&role="+param;
        }else{
            return url+"?role="+param;
        }
    }

    options.corespringUrl = getUrl(com.corespring.players.config.mode, options);

    if (!options.sessionId && !options.itemId) {
      error("Need to specify either itemId or sessionId in options", com.corespring.players.errors.NEED_ITEMID_OR_SESSIONID);
      return;
    }
    /**
     * programmatically submits the item
     *
     * @returns true if successfully submitted, false if error
     **/
    this.submitItem = function () {
      try {
        window.postMessage(JSON.stringify({"message": "submitItem"}), "*");
        return true;
      } catch (e) {
        return false;
      }
    };

    this.saveResponses = function(){
      try{
        window.postMessage(JSON.stringify({"message":"submitItem","isAttempt":false}), "*");
        return true;
      } catch (e) {
        return false;
      }
    }

    var playerRenderFunction = iframePlayerStrategy;
    playerRenderFunction(e, options);
  };

})(this);

