package publicsite.controllers

import play.api.mvc.{Result, Action, Controller}
import models.{Item, ContentCollection, Content}

import com.mongodb.BasicDBObject
import com.mongodb.casbah.Imports._
import models.search.ItemSearch
import play.api.libs.json.{Json, JsArray}
import api.ApiError
import controllers.Utils

object ExampleContent extends Controller {

  val EXAMPLE_CONTENT_COLLECTION_NAME = "Beta Items"

  def items(q: Option[String]) = Action {
    request =>
      def applyQuery(dbquery:MongoDBObject):Result = {
        val items = Item.find(dbquery,MongoDBObject("title" -> 1, "itemType" -> 1, "subjects" -> 1, "gradeLevel" -> 1, "standards" -> 1, "contributorDetails" -> 1))
        Ok(JsArray(Utils.toSeq(items).map(Json.toJson(_))))
      }
      ContentCollection.findOne(MongoDBObject(ContentCollection.name -> EXAMPLE_CONTENT_COLLECTION_NAME)) match {
        case Some(contentCollection) => {
          val initSearch = MongoDBObject(Item.collectionId -> contentCollection.id.toString)
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
