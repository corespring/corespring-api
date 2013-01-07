package basiclti.models

import play.api.mvc.{AnyContent, Request}
import oauth.signpost.http.HttpRequest
import java.net.URLEncoder
import common.controllers.utils.BaseUrl

case class LtiRequestAdapter(request: Request[_], params:Map[String, String]) extends HttpRequest {
  var headers = Map[String,String]()

  def getMethod = request.method


  def getRequestUrl : String = {

    def e(s:String) = URLEncoder.encode(s,"utf-8")

    //TODO: How to decide on http/https?
    val base = BaseUrl(request.asInstanceOf[Request[AnyContent]])
    //val host = if(request.host == null || request.host.isEmpty) "localhost:9000" else request.host
    //val url = "http://" + request.host + request.path
    val url = base + request.path
    if (params.isEmpty){
      url
    } else {
      //Note: only encode the name and value not the = or &
      val paramString = params.toList.map( (kv) => e(kv._1) + "=" + e(kv._2) ).mkString("&")
      url + "?" + paramString
    }
  }

  def setRequestUrl(url: String) {
    // do nothing
  }

  def setHeader(name: String, value: String) {
    headers = headers + (name -> value)
  }

  def getHeader(name: String): String = headers.getOrElse(name, "")

  def getAllHeaders = {
    import scala.collection.JavaConverters._
    headers.asJava
  }

  def getMessagePayload = null

  // setting a hardcoded content type on purpose, otherwise signpost tries to load parse the body content
  def getContentType = "text/html"

  def unwrap() = request
}

