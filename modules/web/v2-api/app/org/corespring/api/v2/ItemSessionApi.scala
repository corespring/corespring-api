package org.corespring.api.v2

import play.api.mvc.{ Action, Controller }
import play.api.libs.json.{ JsString, Json }
import org.bson.types.ObjectId
import scalaz.Scalaz._
import scalaz.{ Failure, Success }
import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.platform.core.models.item.{ItemTransformationCache, PlayItemTransformationCache}
import org.corespring.qtiToV2.transformers.ItemTransformer
import org.corespring.platform.core.services.item.ItemService

trait ItemSessionApi extends Controller {

  def itemService: ItemService
  def sessionService: MongoService

  def create(itemId: VersionedId[ObjectId]) = Action {

    request =>

      def createSessionJson(vid: VersionedId[ObjectId]) = {
        Some(
          Json.obj(
            "_id" -> Json.obj("$oid" -> JsString(ObjectId.get.toString)),
            "itemId" -> JsString(vid.toString)))
      }

      ItemTransformer.updateV2Json(itemId)

      val result = for {
        json <- createSessionJson(itemId).toSuccess("Error creating json")
        sessionId <- sessionService.create(json).toSuccess("Error creating session")
      } yield sessionId

      result match {
        case Success(sessionId) => Ok(Json.obj("id" -> sessionId.toString))
        case Failure(msg) => BadRequest(msg)
      }

  }
}