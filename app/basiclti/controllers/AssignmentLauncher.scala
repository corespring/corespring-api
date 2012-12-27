package basiclti.controllers

import play.api.mvc.{AnyContent, Request, Action}
import play.Logger
import basiclti.models.{LtiLaunchConfiguration, LtiData, Assignment, LtiOAuthConsumer}
import play.api.libs.ws.WS
import play.api.libs.oauth.{RequestToken, ConsumerKey, OAuthCalculator}
import org.bson.types.ObjectId
import models.{Organization, ItemSessionSettings, ItemSession}
import play.api.libs.json.Json._
import basiclti.controllers.routes.{AssignmentPlayer => AssignmentPlayerRoutes}
import basiclti.controllers.routes.{AssignmentLauncher => AssignmentLauncherRoutes}
import oauth.signpost.signature.AuthorizationHeaderSigningStrategy
import common.controllers.utils.BaseUrl
import models.auth.{AccessToken, ApiClient}
import controllers.auth.{OAuthConstants, BaseApi}

/**
 * Handles the launching of corespring items via the LTI 1.1 launch specification.
 * Also supports the canvas 'select_link' selection directive.
 */
object AssignmentLauncher extends BaseApi {

  object LtiKeys {
    val ConsumerKey: String = "oauth_consumer_key"
    val Signature:String = "oauth_signature"
    val Instructor:String = "Instructor"
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

    def consumerFromClient(client:ApiClient) : Option[LtiOAuthConsumer] = {
      val consumer = LtiOAuthConsumer(client)
      consumer.sign(request)
      consumer.setSigningStrategy(new AuthorizationHeaderSigningStrategy())
      Some(consumer)
    }

    val org = for {
      client <- ApiClient.findByKey(clientId)
      consumer <- consumerFromClient(client)
      if (signaturesMatch(request,consumer))
    } yield Organization.findOneById(client.orgId)

    org.getOrElse(None)
  }


  private def signaturesMatch(request:Request[AnyContent], consumer:LtiOAuthConsumer) : Boolean = {
    val requestSignature = request.body.asFormUrlEncoded.get(LtiKeys.Signature).head
    consumer.getOAuthSignature() match {
      case Some(s) =>{
        Logger.debug("signature: " + s)
        Logger.debug("requestSignature: " + requestSignature)
        s.equals(requestSignature)
      }
      case _ => false
    }
  }

  def launch() = Action {
    request =>

      LtiData(request) match {
        case Some(data) => {

          val config : LtiLaunchConfiguration = getOrCreateConfig(data)

          getOrgFromOauthSignature(request) match {
            case Some(org) => {


              val token : AccessToken = AccessToken.getTokenForOrg(org)
              val tokenSession = (OAuthConstants.AccessToken, token.tokenId)

              def isInstructor = data.roles.exists(_ == LtiKeys.Instructor)

              if (isInstructor) {

                Ok( basiclti.views.html.itemChooser(
                    config.id,
                    data.selectionDirective.getOrElse(""),
                    data.returnUrl.getOrElse("")
                  ) ).withSession(tokenSession)
              } else {
                if(config.itemId.isDefined){
                  require(data.outcomeUrl.isDefined, "outcome url must be defined: config id: " + config.id)
                  require(data.resultSourcedId.isDefined, "sourcedid must be defined: config id: " + config.id)
                  require(data.returnUrl.isDefined, "return url must be defined: config id: " + config.id)

                  val updatedConfig = config.addAssignmentIfNew(data.resultSourcedId.get, data.outcomeUrl.get, data.returnUrl.get)
                  val call = AssignmentPlayerRoutes.run(updatedConfig.id, data.resultSourcedId.get)
                  Redirect(call.url).withSession(tokenSession)
                }else {
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

  private def getOrCreateConfig(data: LtiData): LtiLaunchConfiguration = {

    require(data.resourceLinkId.isDefined)

    /**
     * Create a new LaunchConfig
     * @param linkId
     * @return
     */
    def newConfig(linkId:String) : LtiLaunchConfiguration = {
      require(data.oauthConsumerKey.isDefined, "oauth consumer must be defined")
      val client = ApiClient.findByKey(data.oauthConsumerKey.get)

      val out = new LtiLaunchConfiguration(
        resourceLinkId = linkId,
        itemId = None,
        sessionSettings = Some(new ItemSessionSettings()),
        orgId = client.map( _.orgId )
      )
      LtiLaunchConfiguration.insert(out)
      out
    }

    def findByCanvasConfigId(id:String) : LtiLaunchConfiguration = LtiLaunchConfiguration.findOneById(new ObjectId(id)) match {
      case Some(c) => c
      case _ => throw new RuntimeException("A canvas config id was specified but can't be found")
    }

    if (data.selectionDirective == Some("select_link")){
      newConfig("select_link")
    } else {
      data.canvasConfigId match {
        case Some(canvasId) => findByCanvasConfigId(canvasId)
        case _ => {
          val rId = data.resourceLinkId.get
          LtiLaunchConfiguration.findByResourceLinkId(rId) match {
            case Some(config) => config
            case _ => newConfig(rId)
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

  /**
   */
  def process(configId: ObjectId, resultSourcedId: String) = ApiAction {
    request =>

      require(!resultSourcedId.isEmpty, "no resultSourcedId specified - can't process")

      session(configId, resultSourcedId) match {
        case Left(msg) => BadRequest(msg)
        case Right(session) => {

          LtiLaunchConfiguration.findOneById(configId) match {
            case Some(config) => {

              require(config.orgId.isDefined)

              config.assignments.find(_.resultSourcedId == resultSourcedId) match {
                case Some(a) => {
                  val client : Option[ApiClient] = ApiClient.findByOrgId(config.orgId)
                  sendScore(session, a, client)
                }
                case _ => NotFound("Can't find assignment")
              }
            }
            case _ => NotFound("Can't find config")
          }
        }
      }
  }

  /**
   * Send score to the LMS via a signed POST
   * @see https://canvas.instructure.com/doc/api/file.assignment_tools.html
   * @param session
   * @param assignment
   * @param maybeClient
   * @return
   */
  private def sendScore(session: ItemSession, assignment: Assignment, maybeClient : Option[ApiClient] ) = maybeClient match {

    case Some(client) => {

      def sendResultsToPassback(consumer: LtiOAuthConsumer, score: String) = {
        WS.url(assignment.gradePassbackUrl)
          .sign(
          OAuthCalculator(ConsumerKey(consumer.getConsumerKey, consumer.getConsumerSecret),
            RequestToken(consumer.getToken, consumer.getTokenSecret)))
          .withHeaders(("Content-Type", "application/xml"))
          .post(responseXml(assignment.resultSourcedId, score))
      }

      val consumer = LtiOAuthConsumer(client)
      val score = getScore(session)

      def emptyOrNull(s:String) : Boolean = (s == null || s.isEmpty)

      if (emptyOrNull(assignment.gradePassbackUrl)){
        Logger.warn("Not sending passback for assignment: " + assignment.resultSourcedId)
        Ok(toJson(Map("returnUrl" -> assignment.onFinishedUrl)))
      } else {
        sendResultsToPassback(consumer, score).await(10000).fold(
          error => throw new RuntimeException(error.getMessage),
          response => {
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

  def xmlConfiguration = Action {
    request =>
      val url = basiclti.controllers.routes.AssignmentLauncher.launch().url
      val root = BaseUrl(request)
      Ok(xml("item-chooser", "choose an item", root + url, 600, 500)).withHeaders((CONTENT_TYPE, "application/xml"))
  }
}
