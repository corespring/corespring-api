var com = com || {};
com.corespring = com.corespring || {};
com.corespring.TestPlayer = {};

function addDimensionChangeListener(elem) {
  var fn = function(a) {
    var data = JSON.parse(a.data);
    if (data.message == 'dimensionsUpdate') {
      elem.height((data.h+30)+"px");
    }
  }

  if (window.addEventListener) {
    window.addEventListener('message', fn, true);
  }
  else if (window.attachEvent) {
    window.attachEvent('message', fn);
  }
}

var iframePlayerStrategy = function(e, options) {
  var url = options.corespringUrl || "http://www.corespring.org";

  // TODO: decide whether we support this or not
  if (options.itemId) {
    url += "/testplayer/item/" + options.itemId + "/run";
  } else if (options.sessionId) {
    url += "/testplayer/session/" + options.sessionId + "/render";
  }

  e.html("<iframe src='" + url + "' style='width: 100%; height: 100%'></iframe>")
  e.css("width", options.width ? options.width : "600px");

  if (options.autoHeight)
    addDimensionChangeListener(e);
  else
    e.height(options.height ? options.height : "600px");
}

com.corespring.TestPlayer.init = function (element, options, errorCallback) {
  if (!jQuery) {
    errorCallback("jQuery not found");
  }

  var e = $(element);

  if (!e) {
    errorCallback({msg: "Container element not found."});
    return;
  }

  if (!options) {
    errorCallback({msg: "Need to specify options"});
    return;
  }

  if (!options.corespringUrl) {
    errorCallback({msg: "Need to specify corespringUrl in options"});
    return;
  }

  if (!options.sessionId && !options.itemId) {
    errorCallback({msg: "Need to specify either itemId or sessionId in options"});
    return;
  }

  var playerRenderFunction = iframePlayerStrategy;

  playerRenderFunction(e, options);

}