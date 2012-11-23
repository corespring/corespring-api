package basiclti

import models.LtiOAuthConsumer
import play.api.mvc.{Action, Controller}
import play.api.Logger
import oauth.signpost.signature.AuthorizationHeaderSigningStrategy


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
            val url = "testplayer/item/%s/run?access_token=%s".format(data.corespringItemId, common.mock.MockToken)
            Redirect(url)
          }
          case _ => BadRequest("Invalid OAuth signature")
        }
      }
    )
  }

  def launchList = Action { request =>
    LaunchData.buildFromRequest(request,
      List(LaunchData.LtiMessageType, LaunchData.LtiVersion, LaunchData.ResourceLinkId, LaunchData.LaunchPresentationLocale, LaunchData.LaunchPresentationReturnUrl)
    ).fold(
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
            val url = "/public/collection?access_token="+common.mock.MockToken+"&lti_return_url="+data.launchPresentation.returnUrl.get
            Redirect(url)
          }
          case _ => BadRequest("Invalid OAuth signature")
        }
      }
    )
  }
}

