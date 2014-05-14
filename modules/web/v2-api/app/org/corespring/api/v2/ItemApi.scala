package org.corespring.api.v2

import org.bson.types.ObjectId
import org.corespring.api.v2.actions.{ OrgRequest, V2ItemActions }
import org.corespring.api.v2.errors.Errors.{ generalError, errorSaving, noJson }
import org.corespring.api.v2.errors.V2ApiError
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.ItemService
import play.api.libs.json.{ Json, JsValue }
import play.api.mvc.{ AnyContent, Controller }
import scalaz.{ Failure, Success, Validation }

trait ItemApi extends Controller {

  def itemActions: V2ItemActions
  def itemService: ItemService

  private def defaultItem(collectionId: ObjectId): JsValue = {
    Json.obj(
      "collectionId" -> collectionId.toString)
  }

  def create = itemActions.create[AnyContent] { request: OrgRequest[AnyContent] =>

    import scalaz.Scalaz._

    val result: Validation[V2ApiError, Item] = for {
      json <- request.body.asJson.orElse(Some(defaultItem(request.defaultCollection))).toSuccess(noJson)
      item <- json.asOpt[Item].toSuccess(generalError(BAD_REQUEST, "Can't parse json as an Item"))
      vid <- itemService.insert(item).toSuccess(errorSaving)
    } yield {
      item.copy(id = vid)
    }

    result match {
      case Success(item) => Ok(Json.toJson(item))
      case Failure(e) => Status(e.code)(e.message)
    }
  }
}
