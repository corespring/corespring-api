package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.encryption.apiClient.{ EncryptionSuccess, EncryptionResult, ApiClientEncryptionService }
import org.corespring.models.auth.ApiClient
import org.corespring.models.item.PlayerDefinition
import org.corespring.services.OrganizationService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.auth.ApiClientService
import org.corespring.v2.api.services.ScoreService
import org.corespring.v2.auth.SessionAuth
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.Errors.{ generalError, sessionDoesNotContainResponses }
import org.corespring.v2.errors.Errors._
import org.corespring.v2.errors.V2Error
import play.api.Logger
import play.api.libs.json.{ JsObject, JsString, JsValue, Json }
import play.api.mvc.{ RequestHeader, Action, AnyContent }

import scala.concurrent._
import scalaz.Scalaz._
import scalaz.{ Failure, Success, Validation }

case class ItemSessionApiExecutionContext(context: ExecutionContext)

class ItemSessionApi(
  sessionAuth: SessionAuth[OrgAndOpts, PlayerDefinition],
  scoreService: ScoreService,
  orgService: OrganizationService,
  encryptionService: ApiClientEncryptionService,
  apiClientService: ApiClientService,
  sessionCreatedForItem: VersionedId[ObjectId] => Unit,
  apiContext: ItemSessionApiExecutionContext,
  override val getOrgAndOptionsFn: RequestHeader => Validation[V2Error, OrgAndOpts]) extends V2Api {

  override implicit def ec: ExecutionContext = apiContext.context

  private lazy val logger = Logger(classOf[ItemSessionApi])

  /**
   * Creates a new v2 ItemSession in the database.
   *
   * @param itemId  - the item to point to
   *
   * @return json - either:
   *
   *         { "id" -> "$new_session_id" }
   *
   *         Or:
   *         a json representation of a V2Error
   * @see V2Error
   *
   *      ## Authentication
   *
   *      Requires that the request is authenticated. This can be done using the following means:
   *
   *      UserSession authentication (only possible when using the tagger app)
   *      adding an `access_token` query parameter to the call
   *      adding `apiClient` and `playerToken` query parameter to the call
   *
   */
  def create(itemId: VersionedId[ObjectId]) = Action(parse.empty) { implicit request =>
    {
      def createSessionJson(vid: VersionedId[ObjectId], orgAndOpts: OrgAndOpts) = Json.obj(
        "_id" -> Json.obj(
          "$oid" -> JsString(ObjectId.get.toString)),
        "itemId" -> JsString(vid.toString))

      sessionCreatedForItem(itemId)

      val result: Validation[V2Error, JsValue] = for {
        identity <- getOrgAndOptions(request)
        canCreate <- sessionAuth.canCreate(itemId.toString)(identity)
        json <- Success(createSessionJson(itemId, identity))
        sessionId <- if (canCreate)
          sessionAuth.create(json)(identity)
        else
          Failure(generalError("creation failed"))
      } yield Json.obj("id" -> sessionId.toString)

      validationToResult[JsValue](Ok(_))(result)
    }
  }

  private def mapSessionJson(rawJson: JsObject): JsObject = {
    (rawJson \ "_id" \ "$oid").asOpt[String].map { oid =>
      (rawJson - "_id") + ("id" -> JsString(oid))
    }.getOrElse(rawJson)
  }

  /**
   * retrieve a v2 ItemSession in the database.
   *
   * @param sessionId  - the item to point to
   *
   * @return json - either the session json or a json representation of a V2Error
   *
   * @see V2Error
   *
   *      ## Authentication
   *
   *      Requires that the request is authenticated. This can be done using the following means:
   *
   *      UserSession authentication (only possible when using the tagger app)
   *      adding an `access_token` query parameter to the call
   *      adding `apiClient` and `options` query parameter to the call
   *
   */
  def get(sessionId: String) = Action.async { implicit request =>
    Future {
      validationToResult[(SessionAuth.Session, PlayerDefinition)](tuple => Ok(mapSessionJson(tuple._1.as[JsObject]))) {
        for {
          identity <- getOrgAndOptions(request)
          session <- sessionAuth.loadForRead(sessionId)(identity)
        } yield session
      }
    }
  }

  /**
   * Returns the score for the given session.
   * If the session doesn't contain a 'components' object, an error will be returned.
   * @param sessionId
   * @return
   */
  def loadScore(sessionId: String): Action[AnyContent] = Action.async { implicit request =>

    logger.debug(s"function=loadScore sessionId=$sessionId")

    def getComponents(session: JsValue): Option[JsValue] = {
      (session \ "components").asOpt[JsObject]
    }

    Future {
      val out: Validation[V2Error, JsValue] = for {
        identity <- getOrgAndOptions(request)
        sessionAndPlayerDef <- sessionAuth.loadForWrite(sessionId)(identity)
        session <- Success(sessionAndPlayerDef._1)
        playerDef <- Success(sessionAndPlayerDef._2)
        components <- getComponents(session).toSuccess(sessionDoesNotContainResponses(sessionId))
        score <- scoreService.score(playerDef, components)
      } yield score

      validationToResult[JsValue](j => Ok(j))(out)
    }
  }

  def reopen(sessionId: String): Action[AnyContent] = Action.async { implicit request =>
    Future {
      val out: Validation[V2Error, JsValue] = for {
        identity <- getOrgAndOptions(request)
        session <- sessionAuth.reopen(sessionId)(identity)
      } yield session
      validationToResult[JsValue](Ok(_))(out)
    }
  }

  def complete(sessionId: String): Action[AnyContent] = Action.async { implicit request =>
    Future {
      val out: Validation[V2Error, JsValue] = for {
        identity <- getOrgAndOptions(request)
        session <- sessionAuth.complete(sessionId)(identity)
      } yield session
      validationToResult[JsValue](Ok(_))(out)
    }
  }

  /**
   * Clones a session into the preview session so that it may be used for troubleshooting purposes. This API call may
   * only be called by an organization which has access to the session, and returns an api client, encrypted options,
   * the owner organization name, and a cloned session id. These returned parameters allow for a fully-executable clone
   * of the original session.
   */
  def cloneSession(sessionId: String): Action[AnyContent] = Action.async { implicit request =>

    Future {
      val out: Validation[V2Error, JsValue] = for {
        identity <- getOrgAndOptions(request)
        sessionId <- sessionAuth.cloneIntoPreview(sessionId)(identity)
        apiClient <- randomApiClient(identity.org.id).toSuccess(invalidToken(request))
        options <- encryptionService.encrypt(apiClient, ItemSessionApi.clonedSessionOptions.toString).toSuccess(noOrgIdAndOptions(request))
        session <- sessionAuth.loadWithIdentity(sessionId.toString)(identity)
          .map { case (json, _) => withApiClient(withOptions(withOrg(json), options), apiClient) }
      } yield session

      validationToResult[JsValue](Created(_))(out)
    }
  }

  protected def randomApiClient(orgId: ObjectId): Option[ApiClient] = apiClientService.findOneByOrgId(orgId)

  private def withOrg(jsValue: JsValue) = jsValue match {
    case jsObject: JsObject => (jsObject \ "identity" \ "orgId").asOpt[String]
      .map(orgId => orgService.findOneById(new ObjectId(orgId))).flatten match {
        case Some(org) => jsObject ++ Json.obj("organization" -> org.name)
        case _ => jsValue
      }
    case _ => jsValue
  }

  private def withOptions(jsValue: JsValue, result: EncryptionResult) = result match {
    case success: EncryptionSuccess => jsValue match {
      case jsObject: JsObject => jsObject ++
        Json.obj("options" -> success.data)
      case _ => jsValue
    }
    case _ => jsValue
  }

  private def withApiClient(jsValue: JsValue, apiClient: ApiClient) = jsValue match {
    case jsObject: JsObject => jsObject ++ Json.obj("apiClient" -> apiClient.clientId.toString)
    case _ => jsValue
  }

}

object ItemSessionApi {

  val clonedSessionOptions = Json.obj(
    "sessionId" -> "*",
    "itemId" -> "*",
    "mode" -> "gather",
    "expires" -> 0,
    "secure" -> false)

}
