package basiclti.controllers

import basiclti.controllers.routes.{AssignmentLauncher => AssignmentLauncherRoutes}
import basiclti.controllers.routes.{AssignmentPlayer => AssignmentPlayerRoutes}
import basiclti.models._
import common.controllers.utils.BaseUrl
import controllers.auth.{RenderOptions, BaseApi}
import models.Organization
import models.auth.ApiClient
import models.itemSession.{ItemSessionSettings, ItemSession}
import oauth.signpost.signature.AuthorizationHeaderSigningStrategy
import org.bson.types.ObjectId
import play.Logger
import play.api.libs.json.Json
import play.api.libs.json.Json._
import play.api.libs.oauth.ConsumerKey
import play.api.libs.oauth.OAuthCalculator
import play.api.libs.oauth.RequestToken
import play.api.libs.ws.WS
import play.api.mvc.{AnyContent, Request, Action, Session}
import player.rendering.{PlayerCookieKeys, PlayerCookieWriter}
import scala.Left
import scala.Right
import scala.Some
import player.controllers.auth.RequestedAccess
import basiclti.controllers.auth.LtiCookieKeys

/**
 * Handles the launching of corespring items via the LTI 1.1 launch specification.
 * Also supports the canvas 'select_link' selection directive.
 */
object AssignmentLauncher extends BaseApi with PlayerCookieWriter {

  object LtiKeys {
    val ConsumerKey: String = "oauth_consumer_key"
    val Signature: String = "oauth_signature"
    val Instructor: String = "Instructor"
  }

  val defaultSessionSettings = ItemSessionSettings(
    maxNoOfAttempts = 1,
    showFeedback = true,
    highlightCorrectResponse = true,
    highlightUserResponse = true,
    allowEmptyResponses = true
  )

  private def getOrgFromOauthSignature(request: Request[AnyContent]): Option[Organization] = {

    val clientId: String = request.body.asFormUrlEncoded.get(LtiKeys.ConsumerKey).head

    def consumerFromClient(client: ApiClient): Option[LtiOAuthConsumer] = {
      val consumer = LtiOAuthConsumer(client)
      consumer.sign(request)
      consumer.setSigningStrategy(new AuthorizationHeaderSigningStrategy())
      Some(consumer)
    }

    val org = for {
      client <- ApiClient.findByKey(clientId)
      consumer <- consumerFromClient(client)
      if (signaturesMatch(request, consumer))
    } yield Organization.findOneById(client.orgId)

    org.getOrElse(None)
  }


  private def signaturesMatch(request: Request[AnyContent], consumer: LtiOAuthConsumer): Boolean = {
    val requestSignature = request.body.asFormUrlEncoded.get(LtiKeys.Signature).head
    consumer.getOAuthSignature() match {
      case Some(s) => {
        Logger.debug("signature: " + s)
        Logger.debug("requestSignature: " + requestSignature)
        s.equals(requestSignature)
      }
      case _ => false
    }
  }

  def launch() = Action {
    implicit request =>

      LtiData(request) match {
        case Some(data) => {

          val quiz: LtiQuiz = getOrCreateQuiz(data)

          getOrgFromOauthSignature(request) match {
            case Some(org) => {

              /**
               * Note: For Any content hosted in an iframe to support IE we need to add some p3p tags
               * see: http://stackoverflow.com/questions/389456/cookie-blocked-not-saved-in-iframe-in-internet-explorer
               */
              val p3pHeaders = ("P3P", """CP="NOI ADM DEV COM NAV OUR STP"""")

              def buildSession(s: Session, mode : String): Session = {

                s +
                  (PlayerCookieKeys.RENDER_OPTIONS -> Json.toJson(RenderOptions.ANYTHING).toString) +
                  (LtiCookieKeys.QUIZ_ID -> quiz.id.toString) +
                  (PlayerCookieKeys.ORG_ID -> org.id.toString) +
                  (PlayerCookieKeys.ACTIVE_MODE -> mode)
              }


              def isInstructor = data.roles.exists(_ == LtiKeys.Instructor)

              if (isInstructor) {

                Ok(basiclti.views.html.itemChooser(
                  quiz.id,
                  data.selectionDirective.getOrElse(""),
                  data.returnUrl.getOrElse("")
                ))
                  .withSession(buildSession(request.session, RequestedAccess.PREVIEW_MODE))
                  .withHeaders(p3pHeaders)
              } else {
                if (quiz.question.itemId.isDefined) {
                  require(data.outcomeUrl.isDefined, "outcome url must be defined: quiz id: " + quiz.id)
                  require(data.resultSourcedId.isDefined, "sourcedid must be defined: quiz id: " + quiz.id)
                  require(data.returnUrl.isDefined, "return url must be defined: quiz id: " + quiz.id)

                  val updatedConfig = quiz.addParticipantIfNew(data.resultSourcedId.get, data.outcomeUrl.get, data.returnUrl.get)
                  val call = AssignmentPlayerRoutes.run(updatedConfig.id, data.resultSourcedId.get)
                  Redirect(call.url)
                    .withSession(buildSession(request.session, RequestedAccess.ADMINISTER_MODE))
                    .withHeaders(p3pHeaders)
                } else {
                  Ok(basiclti.views.html.itemNotReady())
                }
              }
            }
            case _ => BadRequest("Invalid OAuth signature")
          }
        }
        case _ => BadRequest("Couldn't parse lti launch data")
      }
  }

  private def getOrCreateQuiz(data: LtiData): LtiQuiz = {

    require(data.resourceLinkId.isDefined)

    /**
     * Create a new LaunchConfig
     * @param linkId
     * @return
     */
    def newQuiz(linkId: String): LtiQuiz = {
      require(data.oauthConsumerKey.isDefined, "oauth consumer must be defined")

      val client = ApiClient.findByKey(data.oauthConsumerKey.get)

      require(client.isDefined, "the api client must be defined")

      val quiz = LtiQuiz(
        linkId,
        LtiQuestion(None, ItemSessionSettings()),
        Seq(),
        client.map(_.orgId)
      )
      LtiQuiz.insert(quiz)
      quiz
    }

    def findByCanvasConfigId(id: String): LtiQuiz = LtiQuiz.findOneById(new ObjectId(id)) match {
      case Some(c) => c
      case _ => throw new RuntimeException("A canvas id was specified but can't be found")
    }

    if (data.selectionDirective == Some("select_link")) {
      newQuiz("select_link")
    } else {
      data.canvasConfigId match {
        case Some(canvasId) => findByCanvasConfigId(canvasId)
        case _ => {
          val rId = data.resourceLinkId.get
          LtiQuiz.findByResourceLinkId(rId) match {
            case Some(quiz) => quiz
            case _ => newQuiz(rId)
          }
        }
      }
    }
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


  private def session(id: ObjectId, resultSourcedId: String): Either[String, ItemSession] =
    LtiQuiz.findOneById(id) match {
      case Some(quiz) => {
        quiz.participants.find(_.resultSourcedId == resultSourcedId) match {
          case Some(participant) => {
            ItemSession.findOneById(participant.itemSession) match {
              case Some(session) => Right(session)
              case _ => Left("can't find session")
            }
          }
          case _ => Left("Can't find assignment")
        }
      }
      case _ => Left("Can't find quiz")
    }

  /**
    */
  def process(id: ObjectId, resultSourcedId: String) = ApiAction {
    request =>

      require(!resultSourcedId.isEmpty, "no resultSourcedId specified - can't process")

      session(id, resultSourcedId) match {
        case Left(msg) => BadRequest(msg)
        case Right(session) => {

          LtiQuiz.findOneById(id) match {
            case Some(quiz) => {

              require(quiz.orgId.isDefined)

              quiz.participants.find(_.resultSourcedId == resultSourcedId) match {
                case Some(p) => {

                  quiz.orgId match {
                    case Some(orgId) => {
                      val client: Option[ApiClient] = ApiClient.findOneByOrgId(orgId)
                      sendScore(session, p, client)
                    }
                    case _ => NotFound("Can't find org id")
                  }
                }
                case _ => NotFound("Can't find assignment")
              }
            }
            case _ => NotFound("Can't find quiz")
          }
        }
      }
  }

  /**
   * Send score to the LMS via a signed POST
   * @see https://canvas.instructure.com/doc/api/file.assignment_tools.html
   * @param session
   * @param participant
   * @param maybeClient
   * @return
   */
  private def sendScore(session: ItemSession, participant: LtiParticipant, maybeClient: Option[ApiClient]) = maybeClient match {

    case Some(client) => {

      def sendResultsToPassback(consumer: LtiOAuthConsumer, score: String) = {
        WS.url(participant.gradePassbackUrl)
          .sign(
          OAuthCalculator(ConsumerKey(consumer.getConsumerKey, consumer.getConsumerSecret),
            RequestToken(consumer.getToken, consumer.getTokenSecret)))
          .withHeaders(("Content-Type", "application/xml"))
          .post(responseXml(participant.resultSourcedId, score))
      }

      val consumer = LtiOAuthConsumer(client)
      val score = getScore(session)

      def emptyOrNull(s: String): Boolean = (s == null || s.isEmpty)

      if (emptyOrNull(participant.gradePassbackUrl)) {
        Logger.warn("Not sending passback for assignment: " + participant.resultSourcedId)
        Ok(toJson(Map("returnUrl" -> participant.onFinishedUrl)))
      } else {
        sendResultsToPassback(consumer, score).await(10000).fold(
          error => throw new RuntimeException(error.getMessage),
          response => {
            val returnUrl = response.body match {
              case e: String if e.contains("Invalid authorization header") => {
                AssignmentLauncherRoutes.authorizationError().url
              }
              case _ => participant.onFinishedUrl
            }
            Ok(toJson(Map("returnUrl" -> returnUrl)))
          }
        )
      }
    }
    case _ => throw new RuntimeException("Unable to send score - no client found")
  }

  def authorizationError = Action {
    request =>
      Ok(basiclti.views.html.authorizationError())
  }

  private def getScore(session: ItemSession): String = {
    val (score, maxScore) = ItemSession.getTotalScore(session)
    (score / maxScore).toString
  }

  def xml(title: String, description: String, url: String, width: Int, height: Int) = {
    <cartridge_basiclti_link xmlns="http://www.imsglobal.org/xsd/imslticc_v1p0" xmlns:blti="http://www.imsglobal.org/xsd/imsbasiclti_v1p0" xmlns:lticm="http://www.imsglobal.org/xsd/imslticm_v1p0" xmlns:lticp="http://www.imsglobal.org/xsd/imslticp_v1p0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.imsglobal.org/xsd/imslticc_v1p0 http://www.imsglobal.org/xsd/lti/ltiv1p0/imslticc_v1p0.xsd http://www.imsglobal.org/xsd/imsbasiclti_v1p0 http://www.imsglobal.org/xsd/lti/ltiv1p0/imsbasiclti_v1p0.xsd http://www.imsglobal.org/xsd/imslticm_v1p0 http://www.imsglobal.org/xsd/lti/ltiv1p0/imslticm_v1p0.xsd http://www.imsglobal.org/xsd/imslticp_v1p0 http://www.imsglobal.org/xsd/lti/ltiv1p0/imslticp_v1p0.xsd">
      <blti:title>
        {title}
      </blti:title>
      <blti:description>
        {description}
      </blti:description>
      <blti:extensions platform="canvas.instructure.com">
        <lticm:property name="tool_id">corespring_resource_selection</lticm:property>
        <lticm:property name="privacy_level">anonymous</lticm:property>
        <lticm:options name="resource_selection">
          <lticm:property name="url">
            {url}
          </lticm:property>
          <lticm:property name="text">???</lticm:property>
          <lticm:property name="selection_width">
            {width}
          </lticm:property>
          <lticm:property name="selection_height">
            {height}
          </lticm:property>
        </lticm:options>
      </blti:extensions>
      <cartridge_bundle identifierref="BLTI001_Bundle"/>
      <cartridge_icon identifierref="BLTI001_Icon"/>
    </cartridge_basiclti_link>
  }

  def xmlConfiguration = Action {
    request =>
      val url = basiclti.controllers.routes.AssignmentLauncher.launch().url
      val root = BaseUrl(request)
      Ok(xml("CoreSpring Item Bank", "choose an item", root + url, 700, 600)).withHeaders((CONTENT_TYPE, "application/xml"))
  }
}
