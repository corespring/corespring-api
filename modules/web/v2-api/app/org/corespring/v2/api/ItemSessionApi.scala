package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.auth.SessionAuth
import org.corespring.v2.errors.Errors.{ errorSaving, generalError }
import org.corespring.v2.errors.V2Error
import play.api.libs.json.{ JsString, JsValue, Json }
import play.api.mvc.Action

import scala.concurrent.Future
import scalaz.Scalaz._
import scalaz.{ Failure, Success, Validation }

trait ItemSessionApi extends V2Api {

  def sessionService: MongoService

  def sessionAuth: SessionAuth

  def create(itemId: VersionedId[ObjectId]) = Action.async {
    implicit request =>
      Future {

        def createSessionJson(vid: VersionedId[ObjectId]) = Json.obj(
          "_id" -> Json.obj("$oid" -> JsString(ObjectId.get.toString)),
          "itemId" -> JsString(vid.toString))

        val result: Validation[V2Error, JsValue] = for {
          canCreate <- sessionAuth.canCreate(itemId.toString)
          json <- Success(createSessionJson(itemId))
          sessionId <- if (canCreate)
            sessionService.create(json).toSuccess(errorSaving(s"Error creating session with json: ${json}"))
          else
            Failure(generalError("creation failed"))
        } yield Json.obj("id" -> sessionId.toString)

        validationToResult[JsValue](Ok(_))(result)
      }
  }

  def get(sessionId: String) = Action.async { implicit request =>
    Future {
      validationToResult[(SessionAuth#Session, Item)](tuple => Ok(tuple._1)) {
        sessionAuth.loadForRead(sessionId)
      }
    }
  }

}
