package org.corespring.api.v2

import org.bson.types.ObjectId
import org.corespring.api.v2.actions.{OrgRequest, V2ItemActions}
import org.corespring.api.v2.errors.Errors.{incorrectJsonFormat, generalError, errorSaving, noJson}
import org.corespring.api.v2.errors.V2ApiError
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.ItemService
import play.api.libs.json.{JsObject, Json, JsValue}
import play.api.mvc.{AnyContent, Controller}
import scala.concurrent.{ExecutionContext, Future}
import scalaz.{Failure, Success, Validation}
import org.slf4j.{LoggerFactory, Logger}

trait ItemApi extends Controller {

  implicit def executionContext : ExecutionContext

  def itemActions: V2ItemActions[AnyContent]

  def itemService: ItemService

  protected lazy val logger = LoggerFactory.getLogger("v2Api.ItemApi")

  private def defaultItem(collectionId: ObjectId): JsValue = {
    Json.obj(
      "collectionId" -> collectionId.toString)
  }

  private def cleanJson(raw: JsValue): Option[JsValue] = raw.asOpt[JsObject].map { rawObj =>
    rawObj - "id"
  }

  def create = itemActions.create { request: OrgRequest[AnyContent] =>

    Future {

      logger.trace("create")
      import scalaz.Scalaz._

      val result: Validation[V2ApiError, Item] = for {
        json <- request.body.asJson.orElse(Some(defaultItem(request.defaultCollection))).toSuccess(noJson)
        cleaned <- cleanJson(json).toSuccess(incorrectJsonFormat(json))
        item <- cleaned.asOpt[Item].toSuccess(generalError(BAD_REQUEST, "Can't parse json as an Item"))
        vid <- itemService.insert(item).toSuccess(errorSaving)
      } yield {
        item.copy(id = vid)
      }

      result match {
        case Success(item) => {
          logger.trace(s"return item: $item")
          Ok(Json.toJson(item))
        }
        case Failure(e) => Status(e.code)(e.message)
      }
    }
  }
}
