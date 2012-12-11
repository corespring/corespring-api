package basiclti.models

import oauth.signpost.AbstractOAuthConsumer
import play.api.mvc.{AnyContent, Request}
import java.net.URLDecoder
import models.auth.ApiClient

class LtiOAuthConsumer(consumerKey: String, consumerSecret: String)
  extends AbstractOAuthConsumer(consumerKey, consumerSecret) {

  var wrapped: LtiRequestAdapter = _

  def wrap(obj: Any) = obj match {
    case request: Request[_] => {
      if (request.body.isInstanceOf[AnyContent]) {
        val body = request.body.asInstanceOf[AnyContent]
        val params = body.asFormUrlEncoded.get.filter(v => !v._1.startsWith("oauth_signature")).map(entry => (entry._1, entry._2.headOption.getOrElse("")))
        wrapped = new LtiRequestAdapter(request, params)
        wrapped
      }
      else {
        throw new IllegalArgumentException("Couldn't parse request body")
      }
    }
    case _ => throw new IllegalArgumentException("LtiOAuthConsumer needs an instance of Request")
  }

  def getOAuthSignature(): Option[String] = {
    wrapped.getHeader("Authorization") match {
      case value: String => {
        val start = value.indexOf("oauth_signature=\"") + 17
        val signature = value.substring(start, value.indexOf("\"", start + 1))
        Some(URLDecoder.decode(signature, "utf-8"))
      }
      case _ => None
    }
  }
}

object LtiOAuthConsumer{
  def apply(client:ApiClient) : LtiOAuthConsumer = {
    new LtiOAuthConsumer(client.clientId.toString, client.clientSecret)
  }
}

