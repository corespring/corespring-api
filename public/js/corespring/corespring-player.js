var com = com || {};
com.corespring = com.corespring || {};
com.corespring.players = {};

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
  }

  if (window.addEventListener) {
    window.addEventListener('message', fn, true);
  }
  else if (window.attachEvent) {
    window.attachEvent('message', fn);
  }
}

var iframePlayerStrategy = function (e, options) {
  var url = options.corespringUrl || "http://www.corespring.org";

  // TODO: decide whether we support this or not
  if (options.itemId) {
    url += "/testplayer/item/" + options.itemId + "/run";
  } else if (options.sessionId) {
    url += "/testplayer/session/" + options.sessionId + "/render";
  }

  if (options.token) {
    url += "?access_token=" + options.token;
  }

  e.html("<iframe src='" + url + "' style='width: 100%; height: 100%'></iframe>")
  e.width(options.width ? options.width : "600px");

  if (options.autoHeight)
    addDimensionChangeListener(e);
  else
    e.height(options.height ? options.height : "600px");
};

function getBaseUrl(src) {
  var url = document.createElement('a');
  url.href = src;
  console.log(url);
  return url.protocol + "//" + url.hostname + (url.port ? (":" + url.port) : "");
}

function extractKeyAndBaseUrl() {
  var el;
  $("script").each(function (i, e) {
    if (e.src.indexOf("corespring") >= 0) {
      el = e;
      return false;
    }
  });

  var key, url;
  try {
    url = getBaseUrl(el.src);
    var params = el.src.split("?")[1].split("&");
    $.each(params, function (idx, param) {
      var a = param.split("=");
      if (a[0].toLowerCase()=='key')
        key = a[1];
    });
  } catch (e) {}

  return {
    key: key,
    url: url
  }
}

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

  var keyAndUrl = extractKeyAndBaseUrl();
  if (!keyAndUrl.url) {
    errorCallback({msg: "Invalid CoreSpring player location", code: com.corespring.players.errors.INVALID_PLAYER_LOCATION});
  }

  options.corespringUrl = keyAndUrl.url;

  if (!options.sessionId && !options.itemId) {
    errorCallback({msg: "Need to specify either itemId or sessionId in options", code: com.corespring.players.errors.NEED_ITEMID_OR_SESSIONID});
    return;
  }

  var playerRenderFunction = iframePlayerStrategy;
  playerRenderFunction(e, options);
};
