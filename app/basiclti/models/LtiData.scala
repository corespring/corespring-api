package basiclti.models

import play.api.mvc.{ AnyContent, Request }

case class LtiData(outcomeUrl: Option[String],
  resultSourcedId: Option[String],
  returnUrl: Option[String],
  resourceLinkId: Option[String],
  roles: Seq[String],
  selectionDirective: Option[String],
  oauthConsumerKey: Option[String],
  canvasConfigId: Option[String])

object LtiData {

  object Keys {

    val OutcomeServiceUrl: String = "lis_outcome_service_url"
    val ResultSourcedId: String = "lis_result_sourcedid"
    val LaunchPresentationReturnUrl: String = "launch_presentation_return_url"
    val ResourceLinkId: String = "resource_link_id"
    val Roles: String = "roles"
    val SelectionDirective: String = "selection_directive"
    val CanvasConfigId: String = "canvas_config_id"
    val OAuthConsumerKey: String = "oauth_consumer_key"
    val ContextLabel: String = "context_label"
  }

  def apply(request: Request[AnyContent]): Option[LtiData] = request.body.asFormUrlEncoded match {
    case Some(form) => {

      Some(
        new LtiData(
          outcomeUrl = getString(form.get(Keys.OutcomeServiceUrl)),
          resultSourcedId = getString(form.get(Keys.ResultSourcedId)),
          returnUrl = getString(form.get(Keys.LaunchPresentationReturnUrl)),
          resourceLinkId = getString(form.get(Keys.ResourceLinkId)),
          roles = getSeq(form.get(Keys.Roles)),
          selectionDirective = getString(form.get(Keys.SelectionDirective)),
          oauthConsumerKey = getString(form.get(Keys.OAuthConsumerKey)),
          canvasConfigId = getString(form.get(Keys.CanvasConfigId))))
    }
    case _ => None
  }

  private def getString(s: Option[Seq[String]]): Option[String] = s match {
    case Some(seq) => Some(seq(0))
    case _ => None
  }

  private def getSeq(s: Option[Seq[String]]): Seq[String] = s match {
    case Some(seq) => seq(0).split(",") //.split(",").toSeq()
    case _ => Seq()
  }

}
