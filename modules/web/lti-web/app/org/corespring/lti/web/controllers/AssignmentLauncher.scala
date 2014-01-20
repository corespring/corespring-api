package org.corespring.lti.web.controllers

import oauth.signpost.signature.AuthorizationHeaderSigningStrategy
import org.bson.types.ObjectId
import org.corespring.common.url.BaseUrl
import org.corespring.lti.models._
import org.corespring.lti.web.accessControl.cookies.LtiCookieKeys
import org.corespring.platform.core.controllers.auth.BaseApi
import org.corespring.platform.core.models.Organization
import org.corespring.platform.core.models.auth.ApiClient
import org.corespring.platform.core.models.itemSession.{ ItemSessionSettings, DefaultItemSession, ItemSession }
import org.corespring.player.accessControl.auth.{CheckSessionAccess, TokenizedRequestActionBuilder}
import org.corespring.player.accessControl.cookies.{PlayerCookieWriter, PlayerCookieKeys}
import org.corespring.player.accessControl.models.{RenderOptions, RequestedAccess}
import play.api.libs.json.Json
import play.api.libs.json.Json._
import play.api.libs.oauth.ConsumerKey
import play.api.libs.oauth.OAuthCalculator
import play.api.libs.oauth.RequestToken
import play.api.libs.ws.{Response, WS}
import play.api.mvc._
import scala.Left
import scala.Right
import scala.Some
import scala.concurrent.Future
import scalaz.Scalaz._
import scala.concurrent.{ExecutionContext, Await, Future}
import org.corespring.platform.core.models.Organization
import org.corespring.platform.core.models.auth.ApiClient
import org.corespring.platform.core.models.itemSession.{ ItemSessionSettings, DefaultItemSession, ItemSession }
import org.corespring.player.accessControl.auth.{ CheckSessionAccess, TokenizedRequestActionBuilder }
import org.corespring.player.accessControl.cookies.{ PlayerCookieKeys, PlayerCookieWriter }
import org.corespring.player.accessControl.models.{ RequestedAccess, RenderOptions }

/**
 * Handles the launching of corespring items via the LTI 1.1 launch specification.
 * Also supports the canvas 'select_link' selection directive.
 */
class AssignmentLauncher(auth: TokenizedRequestActionBuilder[RequestedAccess]) extends BaseApi with PlayerCookieWriter {

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
    allowEmptyResponses = true)

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
      if signaturesMatch(request, consumer)
    } yield Organization.findOneById(client.orgId)

    org.getOrElse(None)
  }

  private def signaturesMatch(request: Request[AnyContent], consumer: LtiOAuthConsumer): Boolean = {
    val requestSignature = request.body.asFormUrlEncoded.get(LtiKeys.Signature).head
    consumer.getOAuthSignature() match {
      case Some(s) => {
        logger.debug("signature: " + s)
        logger.debug("requestSignature: " + requestSignature)
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

              def buildSession(s: Session, mode: String): Session = {

                s +
                  (PlayerCookieKeys.renderOptions -> Json.toJson(RenderOptions.ANYTHING).toString) +
                  (LtiCookieKeys.QUIZ_ID -> quiz.id.toString) +
                  (PlayerCookieKeys.orgId -> org.id.toString) +
                  (PlayerCookieKeys.activeMode -> mode)
              }

              def isInstructor = data.roles.exists(_ == LtiKeys.Instructor)

              if (isInstructor) {

                Ok(_root_.org.corespring.lti.web.views.html.itemChooser(
                  quiz.id,
                  data.selectionDirective.getOrElse(""),
                  data.returnUrl.getOrElse("")))
                  .withSession(buildSession(request.session, RequestedAccess.Mode.Preview.toString))
                  .withHeaders(p3pHeaders)
              } else {
                if (quiz.question.itemId.isDefined) {
                  require(data.outcomeUrl.isDefined, "outcome url must be defined: quiz id: " + quiz.id)
                  require(data.resultSourcedId.isDefined, "sourcedid must be defined: quiz id: " + quiz.id)
                  require(data.returnUrl.isDefined, "return url must be defined: quiz id: " + quiz.id)

                  val updatedConfig = quiz.addParticipantIfNew(data.resultSourcedId.get, data.outcomeUrl.get, data.returnUrl.get)
                  val call = _root_.org.corespring.lti.web.controllers.routes.AssignmentPlayer.run(updatedConfig.id, data.resultSourcedId.get)
                  Redirect(call.url)
                    .withSession(buildSession(request.session, RequestedAccess.Mode.Administer.toString))
                    .withHeaders(p3pHeaders)
                } else {
                  Ok(_root_.org.corespring.lti.web.views.html.itemNotReady())
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
    def newQuiz(linkId: String): LtiQuiz = data.oauthConsumerKey.map { key =>

      require(ObjectId.isValid(key.trim), "the consumer key must be a valid object id")

      val client = ApiClient.findByKey(key.trim)

      require(client.isDefined, "the api client must be defined")

      val quiz = LtiQuiz(
        linkId,
        LtiQuestion(None, ItemSessionSettings()),
        Seq(),
        client.map(_.orgId))
      LtiQuiz.insert(quiz)
      quiz
    }.getOrElse {
      throw new IllegalArgumentException("The consumer key must be defined")
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

  def responseXml(sourcedId: String, score: String) = {
    logger.debug("[responseXml with: %s, %s".format(sourcedId, score))

    /**
     * TODO: Add a result data endpoint so that the users response can be seen.
     * <resultData>
     * <url>https://corespring.org</url>
     * </resultData>
     */
    val out = <imsx_POXEnvelopeRequest xmlns="http://www.imsglobal.org/services/ltiv1p1/xsd/imsoms_v1p0">
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
                        <sourcedId>{ sourcedId }</sourcedId>
                      </sourcedGUID>
                      <result>
                        <resultScore>
                          <language>en</language>
                          <textString>{ score }</textString>
                        </resultScore>
                      </result>
                    </resultRecord>
                  </replaceResultRequest>
                </imsx_POXBody>
              </imsx_POXEnvelopeRequest>
    out
  }
  private def session(id: ObjectId, resultSourcedId: String): Either[String, ItemSession] =
    LtiQuiz.findOneById(id) match {
      case Some(quiz) => {
        quiz.participants.find(_.resultSourcedId == resultSourcedId) match {
          case Some(participant) => {
            DefaultItemSession.findOneById(participant.itemSession) match {
              case Some(session) => Right(session)
              case _ => Left("can't find session")
            }
          }
          case _ => Left("Can't find assignment")
        }
      }
      case _ => Left("Can't find quiz")
    }

  def process(quizId: ObjectId, resultSourcedId: String) = session(quizId, resultSourcedId) match {
    case Left(msg) => Action { request =>
      logger.warn("Error processing response: " + msg)
      BadRequest(msg)
    }
    case Right(session) => {

      auth.ValidatedAction(RequestedAccess.asRead(assessmentId = Some(quizId), itemId = Some(session.itemId), sessionId = Some(session.id))) {
        request =>
          import scalaz._

          val result: Validation[String, Future[SimpleResult]] = for {
            q <- LtiQuiz.findOneById(quizId).toSuccess("Can't find Quiz")
            p <- q.participants.find(_.resultSourcedId == resultSourcedId).toSuccess("Can't find participant")
            orgId <- q.orgId.toSuccess("Quiz has no orgId")
            apiClient <- ApiClient.findOneByOrgId(orgId).toSuccess("Can't find ApiClient for org")
          } yield sendScore(session, p, apiClient)

          import ExecutionContext.Implicits.global

          result match {
            case Failure(msg) => {
              this.logger.warn("Error processing response with source id: %s: %s".format(resultSourcedId, msg))
              Future(BadRequest(msg))
            }
            case Success(result) => {
              result
            }

          }
      }
    }
  }

  /**
   * Send score to the LMS via a signed POST
   * @see https://canvas.instructure.com/doc/api/file.assignment_tools.html
   * @return
   */
  private def sendScore(session: ItemSession, participant: LtiParticipant, client: ApiClient): Future[SimpleResult] = {

    import play.api.libs.concurrent.Execution.Implicits._

    def sendResultsToPassback(consumer: LtiOAuthConsumer, score: String) : Future[Response] = {
      logger.debug("Sending the grade passback to: %s".format(participant.gradePassbackUrl))
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
      logger.warn("Not sending passback for assignment: %s".format(participant.resultSourcedId))
      val futureResult: Future[SimpleResult] = Future {
        Ok(toJson(Map("returnUrl" -> participant.onFinishedUrl)))
      }
      futureResult
    } else {

      val response : Future[Response] = sendResultsToPassback(consumer, score)

      import org.corespring.lti.web.controllers.routes.{AssignmentLauncher => AssignmentLauncherRoutes}

      def toResult(r:Response): SimpleResult = {
        val returnUrl = r.body match {
          case e: String if e.contains("Invalid authorization header") => AssignmentLauncherRoutes.error("Unauthorized").url
          case e: String if e.contains("<imsx_codeMajor>unsupported</imsx_codeMajor>") => AssignmentLauncherRoutes.error("Unsupported").url
          case _ => participant.onFinishedUrl
        }
        Ok(toJson(Map("returnUrl" -> returnUrl)))
      }

      def toError(e:Throwable) : Throwable = { throw new RuntimeException(e.getMessage) }
      response.transform(toResult, toError)
    }
  }

  def error(cause: String) = Action(Ok( org.corespring.lti.web.views.html.error(cause)))

  private def getScore(session: ItemSession): String = {
    val (score, maxScore) = DefaultItemSession.getTotalScore(session)
    (score / maxScore).toString
  }

  def xml(title: String, description: String, url: String, width: Int, height: Int) = {
    <cartridge_basiclti_link xmlns="http://www.imsglobal.org/xsd/imslticc_v1p0" xmlns:blti="http://www.imsglobal.org/xsd/imsbasiclti_v1p0" xmlns:lticm="http://www.imsglobal.org/xsd/imslticm_v1p0" xmlns:lticp="http://www.imsglobal.org/xsd/imslticp_v1p0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.imsglobal.org/xsd/imslticc_v1p0 http://www.imsglobal.org/xsd/lti/ltiv1p0/imslticc_v1p0.xsd http://www.imsglobal.org/xsd/imsbasiclti_v1p0 http://www.imsglobal.org/xsd/lti/ltiv1p0/imsbasiclti_v1p0.xsd http://www.imsglobal.org/xsd/imslticm_v1p0 http://www.imsglobal.org/xsd/lti/ltiv1p0/imslticm_v1p0.xsd http://www.imsglobal.org/xsd/imslticp_v1p0 http://www.imsglobal.org/xsd/lti/ltiv1p0/imslticp_v1p0.xsd">
      <blti:title>
        { title }
      </blti:title>
      <blti:description>
        { description }
      </blti:description>
      <blti:extensions platform="canvas.instructure.com">
        <lticm:property name="tool_id">corespring_resource_selection</lticm:property>
        <lticm:property name="privacy_level">anonymous</lticm:property>
        <lticm:options name="resource_selection">
          <lticm:property name="url">
            { url }
          </lticm:property>
          <lticm:property name="text">???</lticm:property>
          <lticm:property name="selection_width">
            { width }
          </lticm:property>
          <lticm:property name="selection_height">
            { height }
          </lticm:property>
        </lticm:options>
      </blti:extensions>
      <cartridge_bundle identifierref="BLTI001_Bundle"/>
      <cartridge_icon identifierref="BLTI001_Icon"/>
    </cartridge_basiclti_link>
  }

  def xmlConfiguration = Action {
    request =>
      val url = org.corespring.lti.web.controllers.routes.AssignmentLauncher.launch().url
      val root = BaseUrl(request)
      Ok(xml("CoreSpring Item Bank", "choose an item", root + url, 700, 600)).withHeaders((CONTENT_TYPE, "application/xml"))
  }
}

object AssignmentLauncher extends AssignmentLauncher(CheckSessionAccess)
