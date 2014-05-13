package org.corespring.api.v2

import org.bson.types.ObjectId
import org.corespring.api.v2.actions.{OrgRequest, V2ItemActions}
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.ItemService
import play.api.libs.json.{Json, JsValue}
import play.api.mvc.{AnyContent, Controller}
import scalaz.{Failure, Success, Validation}

trait ItemApi extends Controller{

  def itemActions : V2ItemActions
  def itemService : ItemService

  private def defaultItem(collectionId: ObjectId) : JsValue = {
    Json.obj(
    "collectionId" -> collectionId.toString
    )
  }

  def create = itemActions.create[AnyContent]{ request : OrgRequest[AnyContent] =>

    import scalaz.Scalaz._

    val result : Validation[String, Item] = for{
      json <- request.body.asJson.orElse(Some(defaultItem(request.defaultCollection))).toSuccess("Can't load item json")
      item <- json.asOpt[Item].toSuccess("Can't create item from json")
      vid <- itemService.insert(item).toSuccess("Error saving the item")
    } yield {
      item.copy(id = vid)
    }

    result match {
      case Success(item) => Ok(Json.toJson(item))
      case Failure(msg) => BadRequest(msg)
    }
  }
}
