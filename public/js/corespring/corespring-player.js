var com = com || {};
com.corespring = com.corespring || {};
com.corespring.players = {};

com.corespring.players.config = {
  mode : "${mode}",
  baseUrl : "${baseUrl}",
  paths: {
    preview : "/item/:itemId/preview",
    render: "/session/:sessionId/render",
    administerItem : "/item/:itemId/administer",
    administerSession : "/session/:sessionId/administer",
    aggregate: "/aggregate/:assessmentId/:itemId/run"
  }
};

console.log("mode is: " + com.corespring.players.config.mode);

com.corespring.players.errors = {
  NEED_OPTIONS: 0,
  NEED_ITEMID_OR_SESSIONID: 2,
  NEED_EMBEDDING_ELEMENT: 3,
  INVALID_PLAYER_LOCATION: 4
};

function addDimensionChangeListener(elem) {
  var fn = function (a) {
    var data = JSON.parse(a.data);
    if (data.message == 'dimensionsUpdate') {
      elem.height((data.h + 30) + "px");
    }
  };

  if (window.addEventListener) {
    window.addEventListener('message', fn, true);
  }
  else if (window.attachEvent) {
    window.attachEvent('message', fn);
  }
}

var iframePlayerStrategy = function (e, options) {
  e.html("<iframe src='" + options.corespringUrl + "' style='width: 100%; height: 100%'></iframe>");
  e.width(options.width ? options.width : "600px");

  if (options.autoHeight)
    addDimensionChangeListener(e);
  else
    e.height(options.height ? options.height : "600px");
};


com.corespring.players.ItemPlayer = function (element, options, errorCallback) {
  if (!jQuery) {
    errorCallback("jQuery not found");
  }

  var e = $(element);

  if (!e) {
    errorCallback({msg: "Container element not found.", code: com.corespring.players.errors.NEED_EMBEDDING_ELEMENT});
    return;
  }

  if (!options) {
    errorCallback({msg: "Need to specify options", code: com.corespring.players.errors.NEED_OPTIONS});
    return;
  }

  var getUrl = function(mode, options){
    var template =  com.corespring.players.config.baseUrl + com.corespring.players.config.paths.preview;
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
