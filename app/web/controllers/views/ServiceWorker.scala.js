
let config = {};

self.addEventListener("install", function(event) {
  self.skipWaiting();
});

self.addEventListener("message", function(event) {
  try {
    const o = JSON.parse(event.data);

    console.log('[message]', o);

    if(o.type === 'set-cdn'){
      config.cdn = o.cdn;
    }

    if(o.type === 'player-info'){
      config.itemId = o.itemId;
      config.session = o.session;
    }

  } catch(e){
    // do nothing
  }
});

const getCdnRedirect = (event) => {

  if(event.request.method !== 'GET'){
    return;
  }

  if(!config.itemId || config.itemId === ''){
    return;
  }

  if(!config.cdn || config.cdn === ''){
    return;
  }

  const loadAsset = /^.*\/v2\/player\/player\/session\/(.*?)\/(.*)/;
  const l = event.request.url.match(loadAsset);
  const filename = l && l[2];

  if (filename && filename !== "index.html") {
    const cdnRequest = `${config.cdn}/v2/player/player/item/${config.itemId}/${filename}`;
    return fetch(cdnRequest);
  }
}


self.addEventListener("fetch", function(event) {
  event.respondWith(
    new Promise((resolve, reject) => {
      if(config){
        var cdnRedirect = getCdnRedirect(event);
        if(cdnRedirect){
          return resolve(cdnRedirect
          .catch(e => {
            console.warn('cdn-redirect failed, fallback to original url:', event.request.url);
            return fetch(event.request);
          }))
        }
      }
      return resolve(fetch(event.request));
    })
  );
});
