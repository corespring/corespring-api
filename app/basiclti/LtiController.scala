package basiclti

import play.api.mvc.{AnyContent, Request, Action, Controller}
import oauth.signpost.AbstractOAuthConsumer
import oauth.signpost.http.HttpRequest
import play.api.Logger
import java.net.{URLDecoder, URLEncoder}
import oauth.signpost.signature.AuthorizationHeaderSigningStrategy


/**
 *
 */
object LtiController extends Controller {

  def launch = Action { request =>
    LaunchData.buildFromRequest(request).fold(
      errors => {
        BadRequest(errors.mkString(","))
      },
      data => {
        // todo: set up a proper way to assign keys/secrets for tool consumers
        val consumer = new LtiOAuthConsumer("1234", "secret")
        consumer.sign(request)
        consumer.setSigningStrategy(new AuthorizationHeaderSigningStrategy())
        val originalSignature = request.body.asFormUrlEncoded.get("oauth_signature").head
        Logger.info("original signature  = " + originalSignature)
        Logger.info("verified signature  = " + consumer.getOAuthSignature().getOrElse("not available"))
        consumer.getOAuthSignature() match {
          case Some(signature) if signature == originalSignature => {
            val url = "testplayer/item/%s/run?access_token=34dj45a769j4e1c0h4wb".format(data.corespringItemId)
            Redirect(url)
          }
          case _ => BadRequest("Invalid OAuth signature")
        }
      }
    )
  }
}

class LtiOAuthConsumer(consumerKey: String, consumerSecret: String) extends AbstractOAuthConsumer(consumerKey, consumerSecret) {
  var wrapped: LtiRequestAdapter = _

  def wrap(obj: Any) = obj match {
    case request: Request[AnyContent] => {
      val params = request.body.asFormUrlEncoded.get.filter(v => !v._1.startsWith("oauth_signature")).map( entry => (entry._1, entry._2.headOption.getOrElse("")))
      wrapped = new LtiRequestAdapter(request, params)
      wrapped
    }
    case _ => throw new IllegalArgumentException("LtiOAuthConsumer needs an instance of Request")
  }

  def getOAuthSignature(): Option[String] = {
    wrapped.getHeader("Authorization") match {
      case value: String => {
        val start = value.indexOf("oauth_signature=\"") + 17
        val signature = value.substring(start , value.indexOf("\"", start + 1))
        Some(URLDecoder.decode(signature, "utf-8"))
      }
      case _ => None
    }
  }
}

case class LtiRequestAdapter(request: Request[AnyContent], params:Map[String, String]) extends HttpRequest {
  var headers = Map[String,String]()

  def getMethod = request.method

  def getRequestUrl = {
    val url = "http://localhost:9000/basiclti"
    val args = if ( !params.isEmpty ) Some(params.map( s => "%s=%s".format(URLEncoder.encode(s._1, "utf-8") ,URLEncoder.encode(s._2, "utf-8"))).mkString("&")) else None
    args.map(url + "?" + _).getOrElse(url)
  }

  def setRequestUrl(url: String) {
    // do nothing
  }

  def setHeader(name: String, value: String) {
    Logger.info("setHeader for %s = %s".format(name, value))
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