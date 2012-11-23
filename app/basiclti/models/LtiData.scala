package basiclti.models

import play.api.mvc.{AnyContent, Request}

case class LtiData(outcomeUrl: Option[String],
                   resultSourcedId: Option[String],
                   returnUrl: Option[String])

object LtiData {
  def apply(request: Request[AnyContent]): Option[LtiData] = request.body.asFormUrlEncoded match {
    case Some(form) => {

      getString(form.get("lis_outcome_service_url")) match {
        case None => None
        case Some(outcomeUrl) =>
          Some(
            new LtiData(
              outcomeUrl = getString(form.get("lis_outcome_service_url")),
              resultSourcedId = getString(form.get("lis_result_sourcedid")),
              returnUrl = getString(form.get("launch_presentation_return_url"))
            )
          )
      }
    }
    case _ => None
  }

  private def getString(s: Option[Seq[String]]): Option[String] = s match {
    case Some(seq) => Some(seq(0))
    case _ => None
  }

}
