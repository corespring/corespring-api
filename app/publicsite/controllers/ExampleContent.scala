package publicsite.controllers

import api.ApiError
import com.mongodb.casbah.Imports._
import controllers.Utils
import models.ContentCollection
import models.item.Item
import models.item.service.{ItemService, ItemServiceImpl, ItemServiceClient}
import models.search.ItemSearch
import play.api.libs.json.{Json, JsArray}
import play.api.mvc.{Result, Action, Controller}


object ExampleContent extends Controller with ItemServiceClient{

  def itemService : ItemService = ItemServiceImpl

  val EXAMPLE_CONTENT_COLLECTION_NAME = "Beta Items"

  def items(q: Option[String]) = Action {
    request =>
      def applyQuery(dbquery:MongoDBObject):Result = {
        val items = itemService.find(dbquery,MongoDBObject("taskInfo.title" -> 1, "taskInfo.itemType" -> 1, "taskInfo.subjects" -> 1, "taskInfo.gradeLevel" -> 1, "standards" -> 1, "contributorDetails" -> 1))
        Ok(JsArray(items.toSeq.map(Json.toJson(_))))
      }
      ContentCollection.findOne(MongoDBObject(ContentCollection.name -> EXAMPLE_CONTENT_COLLECTION_NAME)) match {
        case Some(contentCollection) => {
          val initSearch = MongoDBObject(Item.Keys.collectionId -> contentCollection.id.toString)
          q.map(query => ItemSearch.toSearchObj(query,Some(initSearch))) match {
            case Some(Right(searchobj)) => applyQuery(searchobj)
            case Some(Left(sc)) => sc.error match {
              case None => Ok(JsArray(Seq()))
              case Some(error) => BadRequest(Json.toJson(ApiError.InvalidQuery(error.clientOutput)))
            }
            case None => applyQuery(initSearch)
          }
        }
        case _ => NotFound
      }
  }
}
