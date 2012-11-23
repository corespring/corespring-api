package basiclti.models

import play.api.mvc.{AnyContent, Request}
import oauth.signpost.http.HttpRequest

case class LtiRequestAdapter(request: Request[AnyContent], params:Map[String, String]) extends HttpRequest {
  var headers = Map[String,String]()

  def getMethod = request.method


  def getRequestUrl = {
    "todo"
    /*val url = LaunchData.BaseUrl+request.path
    val args = if ( !params.isEmpty ) Some(params.map( s => "%s=%s".format(URLEncoder.encode(s._1, "utf-8") ,URLEncoder.encode(s._2, "utf-8"))).mkString("&")) else None
    args.map(url + "?" + _).getOrElse(url)*/
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

