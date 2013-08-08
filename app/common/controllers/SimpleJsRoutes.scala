package common.controllers

import play.core.Router.JavascriptReverseRoute
import org.corespring.common.utils.string


trait SimpleJsRoutes {

  /** Create a simple js object what contains the route information.
    * This is preferable to the standard play js route object that gives you a fully namespaced method.
    * All we want is the object name and the method name.
    *
    * Note: the unusual javascript functions you see below (_qS and _wA) are from the play framework.
    * They need to be added for this object to work.
    * @param objectName - the outer name of the object
    * @param routes - the javascript routes as generated by Play
    * @return
    */
  def createSimpleRoutes(objectName:String, routes : JavascriptReverseRoute*) : String = {

    /** strip the namespace - only use the method name */
    def process(jsr : JavascriptReverseRoute) : String = {
      val split = jsr.name.split("\\.").toList
      val shortName = split.last
      val tokens = Map("functionName" -> shortName, "functionBody" -> jsr.f)
      string.interpolate("routesObject.${functionName} = ${functionBody}\n", string.replaceKey(tokens), string.DollarRegex)
    }

    val test =
      """
       //Simple Js Routes - an alternative rendering to the standard Play Js Routes generator
       //1. Init the Play Framework functions on the global scope (so we can created extendable objects
       if( !window["_qS"] ){
         window["_qS"] = function(items){var qs = ''; for(var i=0;i<items.length;i++) {if(items[i]) qs += (qs ? '&' : '') + items[i]}; return qs ? ('?' + qs) : ''};
       }
       if( !window["_wA"] ) {
          window["_wA"] = function(r){return {method:r.method,url:r.url}};
       }

       //2. Now init the routes object if required.
       (function(){
         var routesObject = null;

         if(window["${routeObjectName}"]){
            routesObject = window["${routeObjectName}"]
         } else {
            routesObject = window["${routeObjectName}"] = {};
         }

         ${functionsList}

       })();
      """
      val tokens = Map("routeObjectName" -> objectName, "functionsList" -> routes.map(process).mkString("\n"))
      string.interpolate(test, string.replaceKey(tokens), string.DollarRegex)

  }
}
