(function (root) {
  var com = root.com = root.com || {};

  com.corespring = com.corespring || {};
  com.corespring.players = {};

  var addMessageListener = function (fn) {
    if (window.addEventListener) {
      window.addEventListener('message', fn, true);
    }
    else if (window.attachEvent) {
      window.attachEvent('message', fn);
    } else {
      throw "couldn't add message listener";
    }
  };

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
    UNKNOWN_MODE: 6
  };

  function addDimensionChangeListener(iframe, element) {
    $(iframe).load(function () {
      var $body = $(iframe, window.top.document).contents().find('body');
      var bodyNode = $body.length == 1 ? $body[0] : undefined;
      var prevHeight = 0;
      setInterval(function () {
        try {
          if (bodyNode) {
            var newHeight = $(bodyNode).height();
            if (newHeight == 0) return;
            if (newHeight != prevHeight) {
              $(element).height(newHeight + 30);
              prevHeight = newHeight + 30;
            }
          }
        } catch (ie) {
          console.log(ie);
        }
      }, 100);
    });
  }

  var iframePlayerStrategy = function (e, options) {
    e.html("<iframe src='" + options.corespringUrl + "' style='width: 100%; height: 100%; border: none'></iframe>");
    e.width(options.width ? options.width : "600px");

    if (options.autoHeight)
      addDimensionChangeListener(e.find('iframe'), e);
    else
      e.height(options.height ? options.height : "600px");
  };

  com.corespring.players.ItemProfile = function (element, options, errorCallback, onLoadCallback) {
    var base = com.corespring.players.config.baseUrl;
    options.corespringUrl = base + com.corespring.players.config.paths.profile.replace(":itemId", options.itemId);
    var e = $(element);
    iframePlayerStrategy(e, options);
    e.find('iframe').load(onLoadCallback);
  };

  com.corespring.players.ItemPlayer = function (element, options, errorCallback) {

    var addSessionListener = function (message, callback, dataHandler) {

      dataHandler = (dataHandler || function (s) {
        return s.id;
      });

      addMessageListener(function (event) {
        try {
          var data = JSON.parse(event.data);
          if (data.message == message && data.session) {
            callback(dataHandler(data.session));
          }
        }
        catch (e) {
          console.warn("error parsing: " + event.data);
        }
      });
    };

    var codes = com.corespring.players.errors;

    var error = function (msg, code) {
      errorCallback({msg: msg, code: code});
    };

    if (!jQuery) {
      error("jQuery not found", codes.NEED_JQUERY);
    }

    var e = $(element);

    if (!e) {
      error("Container element not found.", codes.NEED_EMBEDDING_ELEMENT);
      return;
    }

    if (!options) {
      error("Need to specify options", codes.NEED_OPTIONS);
      return;
    }

    if (!options.mode) {
      error("Need a launch mode", codes.NEED_MODE);
      return;
    }

    if (options.onItemSessionCreated) {
      addSessionListener("itemSessionCreated", options.onItemSessionCreated);
    }

    if (options.onItemSessionRetrieved) {
      addSessionListener("itemSessionRetrieved", options.onItemSessionCompleted);
    }

    if (options.onItemSessionCompleted) {
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
          error("Unknown mode", errors.UNKNOWN_MODE);
          break;
      }
      return template
        .replace(":itemId", options.itemId)
        .replace(":sessionId", options.sessionId)
        .replace(":assessmentId", options.assessmentId);
    };

    options.corespringUrl = getUrl(com.corespring.players.config.mode, options);

    if (!options.sessionId && !options.itemId) {
      errorCallback({msg: "Need to specify either itemId or sessionId in options", code: com.corespring.players.errors.NEED_ITEMID_OR_SESSIONID});
      return;
    }

    var playerRenderFunction = iframePlayerStrategy;
    playerRenderFunction(e, options);
  };
})(this);

