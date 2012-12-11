package basiclti.controllers

import play.api.mvc.{AnyContent, Request, Action, Controller}
import play.Logger
import basiclti.models.{LtiLaunchConfiguration, LtiData, Assignment, LtiOAuthConsumer}
import play.api.libs.ws.WS
import play.api.libs.oauth.{RequestToken, ConsumerKey, OAuthCalculator}
import org.bson.types.ObjectId
import com.mongodb.casbah.commons.MongoDBObject
import models.{ItemSessionSettings, ItemSession}
import play.api.libs.json.Json._

import testplayer.controllers.routes.{ItemPlayer => ItemPlayerRoutes}
import basiclti.controllers.routes.{AssignmentPlayer => AssignmentPlayerRoutes}
import basiclti.controllers.routes.{AssignmentLauncher => AssignmentLauncherRoutes}
import oauth.signpost.signature.AuthorizationHeaderSigningStrategy
import common.controllers.utils.BaseUrl

object AssignmentLauncher extends Controller {

  private def tokenize(url: String, token: String) = "%s?access_token=%s".format(url, token)

  val defaultSessionSettings = ItemSessionSettings(
    maxNoOfAttempts = 1,
    showFeedback = true,
    highlightCorrectResponse = true,
    highlightUserResponse = true,
    allowEmptyResponses = true
  )

  private def isSignedCorrectly(request: Request[AnyContent]): Boolean = {

    val requestSignature = request.body.asFormUrlEncoded.get("oauth_signature").head

    val consumer = new LtiOAuthConsumer("1234", "secret")
    consumer.sign(request)
    consumer.setSigningStrategy(new AuthorizationHeaderSigningStrategy())

    consumer.getOAuthSignature() match {
      case Some(signature) => {
        Logger.debug("signature: " + signature)
        Logger.debug("requestSignature: " + requestSignature)
        signature.equals(requestSignature)
      }
      case _ => false
    }
  }

  def launch() = Action {
    request =>

      LtiData(request) match {
        case Some(data) => {
          val config = getOrCreateConfig(data)

          if (data.roles.exists(_ == "Instructor")) {

            Ok(
              basiclti.views.html.itemChooser(
                config.id,
                data.selectionDirective.getOrElse(""),
                data.returnUrl.getOrElse("")
              )
            )

          } else {
            require(data.outcomeUrl.isDefined, "no outcome url is defined")
            require(data.resultSourcedId.isDefined, "sourcedid is defined")
            val updatedConfig = config.addAssignment(data.resultSourcedId.get, data.outcomeUrl.get, data.returnUrl.get)
            val call = AssignmentPlayerRoutes.run(updatedConfig.id, data.resultSourcedId.get)
            Redirect(call.url)
          }
        }
        case _ => BadRequest("bad launch data")
      }
  }

  private def getOrCreateConfig(data: LtiData): LtiLaunchConfiguration = data.resourceLinkId match {
    case Some(id) => {
      LtiLaunchConfiguration.findByResourceLinkId(id) match {
        case Some(config) => config
        case _ => {
          val newConfig = new LtiLaunchConfiguration(
            resourceLinkId = id,
            itemId = None,
            sessionSettings = Some(new ItemSessionSettings()))
          LtiLaunchConfiguration.create(newConfig)
          newConfig
        }
      }
    }
    case _ => throw new RuntimeException("no link id specified")
  }




  def responseXml(sourcedId: String, score: String) = <imsx_POXEnvelopeRequest>
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
            <sourcedId>
              {sourcedId}
            </sourcedId>
          </sourcedGUID>
          <result>
            <resultScore>
              <language>en</language>
              <textString>
                {score}
              </textString>
            </resultScore>
          </result>
        </resultRecord>
      </replaceResultRequest>
    </imsx_POXBody>
  </imsx_POXEnvelopeRequest>


  private def assignment(configId:ObjectId, resultSourcedId:String) : Option[Assignment] = LtiLaunchConfiguration.findOneById(configId) match {
    case Some(config) => config.assignments.find(_.resultSourcedId == resultSourcedId)
    case _ => None
  }

  private def session(configId: ObjectId, resultSourcedId: String): Either[String, ItemSession] = LtiLaunchConfiguration.findOneById(configId) match {
    case Some(config) => {
      config.assignments.find(_.resultSourcedId == resultSourcedId) match {
        case Some(assignment) => {
          ItemSession.findOneById(assignment.itemSessionId) match {
            case Some(session) => Right(session)
            case _ => Left("can't find session")
          }
        }
        case _ => Left("Can't find assignment")
      }
    }
    case _ => Left("Can't find config")
  }

  private def sendScore(session:ItemSession, assignment:Assignment) = {

    val consumer = new LtiOAuthConsumer("1234", "secret")
    val score = getScore(session)

    def sendResultsToPassback = {
      WS.url(assignment.gradePassbackUrl)
        .sign(
        OAuthCalculator(ConsumerKey("1234", "secret"),
          RequestToken(consumer.getToken, consumer.getTokenSecret)))
        .withHeaders(("Content-Type", "application/xml"))
        .post(responseXml(assignment.resultSourcedId, score))
    }

    sendResultsToPassback.await(10000).fold(
      error => throw new RuntimeException(error.getMessage),
      response => {
        Logger.debug(response.body)
        val returnUrl = response.body match {
          case e: String if e.contains("Invalid authorization header") => {
            AssignmentLauncherRoutes.authorizationError().url
          }
          case _ => assignment.onFinishedUrl
        }
        Ok(toJson(Map("returnUrl" -> returnUrl)))
      }
    )
  }

  /**
   * TODO: token secret
   * @return
   */
  def process(configId: ObjectId, resultSourcedId:String) = Action {
    request =>
      session(configId, resultSourcedId) match {
        case Left(msg) => BadRequest(msg)
        case Right(session) => {
          assignment(configId, resultSourcedId) match {
            case Some(a) => sendScore(session, a)
            case _ => BadRequest("Can't find assignment")
          }
        }
      }
  }

  def authorizationError = Action {
    request =>
      Ok(basiclti.views.html.authorizationError())
  }

  private def getScore(session: ItemSession): String = {
    val (score, maxScore) = ItemSession.getTotalScore(session)
    (score / maxScore).toString
  }


  /**
   * Just for development - to be removed.
   * @return
   */
  def mockLauncher = Action{ request =>
    val url = basiclti.controllers.routes.AssignmentLauncher.launch().url
    Ok(basiclti.views.html.dev.launchItemChooser(url))
  }


  def xml(title:String, description:String, url:String, width:Int, height:Int) = {
    <cartridge_basiclti_link xmlns="http://www.imsglobal.org/xsd/imslticc_v1p0" xmlns:blti="http://www.imsglobal.org/xsd/imsbasiclti_v1p0" xmlns:lticm="http://www.imsglobal.org/xsd/imslticm_v1p0" xmlns:lticp="http://www.imsglobal.org/xsd/imslticp_v1p0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.imsglobal.org/xsd/imslticc_v1p0 http://www.imsglobal.org/xsd/lti/ltiv1p0/imslticc_v1p0.xsd http://www.imsglobal.org/xsd/imsbasiclti_v1p0 http://www.imsglobal.org/xsd/lti/ltiv1p0/imsbasiclti_v1p0.xsd http://www.imsglobal.org/xsd/imslticm_v1p0 http://www.imsglobal.org/xsd/lti/ltiv1p0/imslticm_v1p0.xsd http://www.imsglobal.org/xsd/imslticp_v1p0 http://www.imsglobal.org/xsd/lti/ltiv1p0/imslticp_v1p0.xsd">
      <blti:title>{title}</blti:title>
      <blti:description>{description}</blti:description>
      <blti:extensions platform="canvas.instructure.com">
        <lticm:property name="tool_id">corespring_resource_selection</lticm:property>
        <lticm:property name="privacy_level">anonymous</lticm:property>
        <lticm:options name="resource_selection">
          <lticm:property name="url">{url}</lticm:property>
          <lticm:property name="text">???</lticm:property>
          <lticm:property name="selection_width">{width}</lticm:property>
          <lticm:property name="selection_height">{height}</lticm:property>
        </lticm:options>
      </blti:extensions>
      <cartridge_bundle identifierref="BLTI001_Bundle"/>
      <cartridge_icon identifierref="BLTI001_Icon"/>
    </cartridge_basiclti_link>
  }

  def xmlConfiguration = Action{ request =>
    val url = basiclti.controllers.routes.AssignmentLauncher.launch().url
    val root = BaseUrl(request)
    Ok(xml("item-chooser", "choose an item", root + url, 600, 500)).withHeaders((CONTENT_TYPE, "application/xml"))
  }
}
