package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.container.components.outcome.ScoreProcessor
import org.corespring.container.components.response.OutcomeProcessor
import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.auth.SessionAuth
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.Errors.{ noJson, errorSaving, generalError }
import org.corespring.v2.errors.V2Error
import play.api.libs.json.{ JsObject, JsString, JsValue, Json }
import play.api.mvc.{ AnyContent, Action }

import scala.concurrent._
import scalaz.Scalaz._
import scalaz.{ Failure, Success, Validation }

trait ItemSessionApi extends V2Api {

  def sessionService: MongoService

  def sessionAuth: SessionAuth[OrgAndOpts]

  def outcomeProcessor: OutcomeProcessor

  def scoreProcessor: ScoreProcessor

  /**
   * A session has been created for an item with the given item id.
   * @param itemId
   */
  def sessionCreatedForItem(itemId: VersionedId[ObjectId]): Unit

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
  def create(itemId: VersionedId[ObjectId]) = Action.async(parse.empty) { implicit request =>
    Future {

      def createSessionJson(vid: VersionedId[ObjectId]) = Json.obj(
        "_id" -> Json.obj("$oid" -> JsString(ObjectId.get.toString)),
        "itemId" -> JsString(vid.toString))

      sessionCreatedForItem(itemId)

      val result: Validation[V2Error, JsValue] = for {
        identity <- getOrgIdAndOptions(request)
        canCreate <- sessionAuth.canCreate(itemId.toString)(identity)
        json <- Success(createSessionJson(itemId))
        sessionId <- if (canCreate)
          sessionService.create(json).toSuccess(errorSaving(s"Error creating session with json: ${json}"))
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
      validationToResult[(SessionAuth.Session, Item)](tuple => Ok(mapSessionJson(tuple._1.as[JsObject]))) {
        for {
          identity <- getOrgIdAndOptions(request)
          session <- sessionAuth.loadForRead(sessionId)(identity)
        } yield session
      }
    }
  }

  def loadScore(sessionId: String): Action[AnyContent] = Action.async { implicit request =>

    def withResponses(session: JsValue): Option[JsValue] = {
      (session \ "components").asOpt[JsObject].map(_ => session)
    }

    Future {
      val out: Validation[V2Error, JsValue] = for {
        identity <- getOrgIdAndOptions(request)
        sessionAndItem <- sessionAuth.loadForWrite(sessionId)(identity)
        session <- Success(sessionAndItem._1)
        item <- Success(sessionAndItem._2)
        sessionWithResponses <- withResponses(session).toSuccess(generalError(s"This session doesn't have any saved respons"))
        playerDefinition <- item.playerDefinition.toSuccess(generalError("This item has no player definition"))
      } yield {
        val itemJson = Json.toJson(playerDefinition)
        loadScore(itemJson, sessionWithResponses)
      }
      validationToResult[JsValue](j => Ok(j))(out)
    }
  }

  def checkScore(sessionId: String): Action[AnyContent] = Action.async { implicit request =>
    Future {
      val out: Validation[V2Error, JsValue] = for {
        identity <- getOrgIdAndOptions(request)
        answers <- request.body.asJson.toSuccess(noJson)
        sessionAndItem <- sessionAuth.loadForWrite(sessionId)(identity)
        item <- Success(sessionAndItem._2)
        playerDefinition <- item.playerDefinition.toSuccess(generalError("This item has no player definition"))
      } yield {
        val itemJson = Json.toJson(playerDefinition)
        loadScore(itemJson, answers)
      }
      validationToResult[JsValue](j => Ok(j))(out)
    }
  }

  private def loadScore(item: JsValue, answer: JsValue): JsValue = {
    val outcome = outcomeProcessor.createOutcome(item, answer, Json.obj())
    scoreProcessor.score(item, item, outcome)
  }

}
