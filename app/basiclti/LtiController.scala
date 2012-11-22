package basiclti

import play.api.mvc.{AnyContent, Request, Controller, Action}
import play.Logger
import java.net.{URLDecoder, URLEncoder}
import oauth.signpost.http.HttpRequest
import oauth.signpost.AbstractOAuthConsumer
import play.api.libs.ws.WS
import play.api.libs.oauth.{RequestToken, ConsumerKey, OAuthCalculator}


object LtiController extends Controller {
  def launch = Action(request => Ok(""))
  
  def assessmentLaunch = Action{ request =>

   val call = basiclti.routes.LtiController.assessmentPlay()
    val sessionInfo = info( request, Seq(
      "lis_outcome_service_url",
      "lis_result_sourcedid",
      "lis_person_name_full",
      "lis_person_contact_email_primary")
    )

    Logger.info(sessionInfo.toString())

    if(sessionInfo.filter(_._2 == "?").length > 0 ){
      BadRequest("you are missing some parameters - here's whats missing: " + sessionInfo.toString() )
    } else {
      Redirect(call.url).withSession(sessionInfo : _*)
    }
  }
  
  def assessmentPlay = Action{ request =>
    Ok(basiclti.views.html.assessmentPlay())
  }


   def assessmentProcess = Action{ request =>

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


  def launchList = Action(request => Ok(""))

  private def info(request : Request[AnyContent], fields : Seq[String]) : Seq[(String,String)] = request.body.asFormUrlEncoded match {
      case Some(data) => {
        Logger.info(data.toString())
        fields.map { k =>
          data.getOrElse(k, Seq()) match {
            case Seq(v) => Some((k, v))
            case _ => None
          }
        }.flatten
      }
      case _ => Seq()
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
    val url = LaunchData.BaseUrl+request.path
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