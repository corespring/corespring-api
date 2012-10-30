var TestPlayerRoutes = {}; (function(_root){
    var _nS = function(c,f,b){var e=c.split(f||"."),g=b||_root,d,a;for(d=0,a=e.length;d<a;d++){g=g[e[d]]=g[e[d]]||{}}return g}
    var _qS = function(items){var qs = ''; for(var i=0;i<items.length;i++) {if(items[i]) qs += (qs ? '&' : '') + items[i]}; return qs ? ('?' + qs) : ''}
    var _s = function(p,s){return p+((s===true||(s&&s.secure))?'s':'')+'://'}
    var _wA = function(r){return {ajax:function(c){c.url=r.url;c.type=r.method;return jQuery.ajax(c)}, method:r.method,url:r.url,absoluteURL: function(s){return _s('http',s)+'localhost:9000'+r.url},webSocketURL: function(s){return _s('ws',s)+'localhost:9000'+r.url}}}
    _nS('api.v1.ItemSessionApi'); _root.api.v1.ItemSessionApi.update =
        function(itemId,sessionId,action) {
            return _wA({method:"PUT", url:"/api/v1/items/" + (function(k,v) {return v})("itemId", itemId) + "/sessions/" + (function(k,v) {return v})("sessionId", sessionId) + _qS([(action == null ? null : (function(k,v) {return encodeURIComponent(k)+'='+encodeURIComponent(v)})("action", action))])})
        }

    _nS('api.v1.ItemSessionApi'); _root.api.v1.ItemSessionApi.get =
        function(itemId,sessionId) {
            return _wA({method:"GET", url:"/api/v1/items/" + (function(k,v) {return v})("itemId", itemId) + "/sessions/" + (function(k,v) {return v})("sessionId", sessionId)})
        }

    _nS('api.v1.ItemSessionApi'); _root.api.v1.ItemSessionApi.create =
        function(itemId) {
            return _wA({method:"POST", url:"/api/v1/items/" + (function(k,v) {return v})("itemId", itemId) + "/sessions"})
        }

})(TestPlayerRoutes);