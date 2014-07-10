package org.corespring.api.v2

import scala.concurrent.Future

import org.bson.types.ObjectId
import org.corespring.api.v2.errors.V2ApiError
import org.corespring.api.v2.errors.Errors.generalError
import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.auth.SessionAuth
import play.api.libs.json.{ JsString, JsValue, Json }
import play.api.mvc._
import scalaz.{ Failure, Success, Validation }
import scalaz.Scalaz._

trait ItemSessionApi extends V2Api {

  def sessionService: MongoService

  def sessionAuth: SessionAuth

  def r[A](mkJson: A => JsValue)(v: Validation[String, A]): SimpleResult = v match {
    case Success(d) => Ok(mkJson(d))
    case Failure(msg) => Status(UNAUTHORIZED)(msg)
  }

  def v2r(v: Validation[V2ApiError, JsValue]) = v match {
    case Success(d) => Ok(d)
    case Failure(err) => Status(err.code)(err.message)
  }

  val jr = r[(SessionAuth#Session, Item)]((tuple: (JsValue, _)) => tuple._1) _

  def create(itemId: VersionedId[ObjectId]) = Action.async {
    implicit request =>
      Future {

        def createSessionJson(vid: VersionedId[ObjectId]) = Json.obj(
          "_id" -> Json.obj("$oid" -> JsString(ObjectId.get.toString)),
          "itemId" -> JsString(vid.toString))

        val result: Validation[V2ApiError, JsValue] = for {
          canCreate <- sessionAuth.canCreate(itemId.toString).leftMap(s => generalError(BAD_REQUEST, s))
          json <- Success(createSessionJson(itemId))
          sessionId <- if (canCreate)
            sessionService.create(json).toSuccess(generalError(BAD_REQUEST, s"Error creating session with json: ${json}"))
          else
            Failure(generalError(BAD_REQUEST, "creation failed"))
        } yield Json.obj("id" -> sessionId.toString)

        v2r(result)
      }
  }

  def get(sessionId: String) = Action.async { implicit request =>
    Future {
      jr(sessionAuth.loadForRead(sessionId))
    }
  }

}