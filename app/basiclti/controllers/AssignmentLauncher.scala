package basiclti.controllers

import play.api.mvc.{AnyContent, Request, Action, Controller}
import play.Logger
import basiclti.models.LtiOAuthConsumer
import play.api.libs.ws.WS
import play.api.libs.oauth.{RequestToken, ConsumerKey, OAuthCalculator}

object AssignmentLauncher extends Controller {

  private def info(request : Request[AnyContent], fields : Seq[String]) : Map[String,String] = request.body.asFormUrlEncoded match {
    case Some(data) => {
      Logger.info(data.toString())
      fields.map { k =>
        data.get(k) match {
          case Seq(v) => Some((k, v.toString))
          case _ => None
        }
      }.flatten.toMap
    }
    case _ => Map()
  }

  def launch = Action{ request =>

    val call = basiclti.routes.LtiController.assessmentPlay()
    val sessionInfo = info( request, Seq(
      "lis_outcome_service_url",
      "lis_result_sourcedid",
      "lis_person_name_full",
      "lis_person_contact_email_primary")
    )

    if( !sessionInfo.get("lis_result_sourcedid").isDefined || !sessionInfo.get("lis_outcome_service_url").isDefined ){
      Logger.info("you're a teacher!")
      Ok("you're a teacher - in this mode you'll just play with the assessment but there's no marks")
    } else {
      Redirect(call.url).withSession(sessionInfo.toSeq : _*)
    }
  }


  def play = Action{ request =>
    Ok(basiclti.views.html.assessmentPlay())
  }

  def process = Action{ request =>

    val sourcedId = request.session.get("lis_result_sourcedid").get
    val reportUrl = request.session.get("lis_outcome_service_url").get
    val score = "0.1"

    val responseXml = <imsx_POXEnvelopeRequest>
      <imsx_POXHeader>
        <imsx_POXRequestHeaderInfo>
          <imsx_version>V1.0</imsx_version>
          <imsx_messageIdentifier>12341234</imsx_messageIdentifier>
        </imsx_POXRequestHeaderInfo>
      </imsx_POXHeader>
      <imsx_POXBody>
        <replaceResultRequest>
          <resultRecord>
            <sourcedGUID>
              <sourcedId>{sourcedId}</sourcedId>
            </sourcedGUID>
            <result>
              <resultScore>
                <language>en</language>
                <textString>{score}</textString>
              </resultScore>
            </result>
          </resultRecord>
        </replaceResultRequest>
      </imsx_POXBody>
    </imsx_POXEnvelopeRequest>
    val consumer = new LtiOAuthConsumer("1234", "secret")

    val call = WS.url(reportUrl)
      .sign(
      OAuthCalculator(ConsumerKey("1234","secret"),
        RequestToken(consumer.getToken, consumer.getTokenSecret)))
      .withHeaders(("Content-Type", "application/xml"))
      .post(responseXml)

    call.await(10000).fold(
      error => throw new RuntimeException( error.getMessage ) ,
      response => Ok(basiclti.views.html.assessmentResponse(response.body))
    )
  }
}
