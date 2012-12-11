package basiclti.models

import play.api.mvc.{AnyContent, Request}

case class LtiData(outcomeUrl: Option[String],
                   resultSourcedId: Option[String],
                   returnUrl: Option[String],
                   resourceLinkId: Option[String],
                   roles: Seq[String],
                   selectionDirective: Option[String],
                   oauthConsumerKey:Option[String])

object LtiData {
  def apply(request: Request[AnyContent]): Option[LtiData] = request.body.asFormUrlEncoded match {
    case Some(form) => {

      Some(
        new LtiData(
          outcomeUrl = getString(form.get("lis_outcome_service_url")),
          resultSourcedId = getString(form.get("lis_result_sourcedid")),
          returnUrl = getString(form.get("launch_presentation_return_url")),
          resourceLinkId = getString(form.get("resource_link_id")),
          roles = getSeq(form.get("roles")),
          selectionDirective = getString(form.get("selection_directive")),
          oauthConsumerKey = getString(form.get("oauth_consumer_key"))
        )
      )
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
