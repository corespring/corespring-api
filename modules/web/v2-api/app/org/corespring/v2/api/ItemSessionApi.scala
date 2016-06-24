package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.encryption.apiClient.{ ApiClientEncryptionService, EncryptionSuccess }
import org.corespring.models.item.PlayerDefinition
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.OrganizationService
import org.corespring.v2.actions.V2Actions
import org.corespring.v2.api.services.ScoreService
import org.corespring.v2.auth.SessionAuth
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.Errors._
import org.corespring.v2.errors.V2Error
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.Logger
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent._
import scalaz.Scalaz._
import scalaz.{ Failure, Success, Validation }

case class ItemSessionApiExecutionContext(context: ExecutionContext)

class ItemSessionApi(
  actions: V2Actions,
  sessionAuth: SessionAuth[OrgAndOpts, PlayerDefinition],
  orgService: OrganizationService,
  encryptionService: ApiClientEncryptionService,
  sessionCreatedForItem: VersionedId[ObjectId] => Unit,
  apiContext: ItemSessionApiExecutionContext) extends V2Api with ValidationToResultLike {

  override implicit def ec: ExecutionContext = apiContext.context

  private lazy val logger = Logger(classOf[ItemSessionApi])

  /**
   * Creates a new v2 ItemSession in the database.
   *
   * @param itemId  - the item to point to
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
  def create(itemId: VersionedId[ObjectId]) = actions.Org.async(parse.empty) { implicit request =>
    Future {
      def createSessionJson(vid: VersionedId[ObjectId], orgAndOpts: OrgAndOpts) =
        Json.obj("itemId" -> JsString(vid.toString))

      sessionCreatedForItem(itemId)

      val result: Validation[V2Error, JsValue] = for {
        canCreate <- sessionAuth.canCreate(itemId.toString)(request.orgAndOpts)
        json <- Success(createSessionJson(itemId, request.orgAndOpts))
        sessionId <- if (canCreate)
          sessionAuth.create(json)(request.orgAndOpts)
        else
          Failure(generalError("creation failed"))
      } yield Json.obj("id" -> sessionId.toString)

      result.toSimpleResult()
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
   * @return json - either the session json or a json representation of a V2Error
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
  def get(sessionId: String) = actions.Org.async { implicit request =>
    Future {
      sessionAuth.loadForRead(sessionId)(request.orgAndOpts).map {
        case (json, _) => mapSessionJson(json.as[JsObject])
      }.toSimpleResult()
    }
  }

//<<<<<<< HEAD
//  /**
//   * Returns the score for the given session.
//   * If the session doesn't contain a 'components' object, an error will be returned.
//   *
//   * @param sessionId
//   * @return
//   */
//  def loadScore(sessionId: String): Action[AnyContent] = actions.Org.async { implicit request =>
//
//    logger.debug(s"function=loadScore sessionId=$sessionId")
//
//    def getComponents(session: JsValue): Option[JsValue] = {
//      (session \ "components").asOpt[JsObject]
//    }
//
//    Future {
//      val out: Validation[V2Error, JsValue] = for {
//        sessionAndPlayerDef <- sessionAuth.loadForWrite(sessionId)(request.orgAndOpts)
//        session <- Success(sessionAndPlayerDef._1)
//        playerDef <- Success(sessionAndPlayerDef._2)
//        components <- getComponents(session).toSuccess(sessionDoesNotContainResponses(sessionId))
//        score <- scoreService.score(playerDef, components)
//      } yield score
//
//      out.toSimpleResult()
//    }
//  }
//=======
//>>>>>>> develop

  def reopen(sessionId: String): Action[AnyContent] = actions.Org.async { implicit request =>
    Future {
      sessionAuth.reopen(sessionId)(request.orgAndOpts).toSimpleResult()
    }
  }

  def complete(sessionId: String): Action[AnyContent] = actions.Org.async { implicit request =>
    Future {
      sessionAuth.complete(sessionId)(request.orgAndOpts).toSimpleResult()
    }
  }

  def orgCount(orgId: ObjectId, month: String) = actions.RootOrg.async { implicit request =>
    implicit def dateTimeOrdering: Ordering[DateTime] = Ordering.fromLessThan(_ isBefore _)
    val monthDate = DateTimeFormat.forPattern("MM-yyyy").parseDateTime(month)
    Future {
      sessionAuth.orgCount(orgId, monthDate)(request.orgAndOpts).map { m =>
        JsArray(m.toSeq.sortBy(_._1).map {
          case (d, v) => Json.obj(
            "date" -> DateTimeFormat.forPattern("MM/dd").print(d),
            "count" -> v)
        })
      }.toSimpleResult()
    }
  }

  /**
   * Clones a session into the preview session so that it may be used for troubleshooting purposes. This API call may
   * only be called by an organization which has access to the session, and returns an api client, encrypted options,
   * the owner organization name, and a cloned session id. These returned parameters allow for a fully-executable clone
   * of the original session.
   */
  def cloneSession(sessionId: String): Action[AnyContent] = actions.OrgAndApiClient.async { implicit request =>
    Future {
      val out: Validation[V2Error, JsValue] = for {
        apiClient <- Success(request.apiClient)
        sessionId <- sessionAuth.cloneIntoPreview(sessionId)(request.orgAndOpts)
        options <- encryptionService.encrypt(apiClient, ItemSessionApi.clonedSessionOptions.toString).toSuccess(noOrgIdAndOptions(request))
        session <- sessionAuth.loadWithIdentity(sessionId.toString)(request.orgAndOpts)
          .map {
            case (json, _) => {
              json.asOpt[JsObject].getOrElse(Json.obj()) ++
                Json.obj(
                  "apiClient" -> request.apiClient.clientId.toString,
                  "organization" -> request.org.name) ++
                  (options match {
                    case s: EncryptionSuccess => Json.obj("options" -> s.data)
                    case _ => Json.obj()
                  })
            }
          }
      } yield session
      out.toSimpleResult(CREATED)
    }
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
