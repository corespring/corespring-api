package web.controllers

import play.api.mvc.{Action, Controller}
import models.{Item, ContentCollection, Content}

import com.mongodb.BasicDBObject
import play.api.libs.json.Json
import com.novus.salat.dao.SalatMongoCursor

object ExampleContent extends Controller {

  def index = Action {
    request =>
      Ok(web.views.html.exampleContent())
  }

  val EXAMPLE_CONTENT_COLLECTION_NAME = "Beta Items"

  def items = Action {
    request =>
      val search = new BasicDBObject()
      search.put("name", EXAMPLE_CONTENT_COLLECTION_NAME)
      ContentCollection.findOne(search) match {
        case Some(contentCollection) => {
          val itemSearch = new BasicDBObject()
          itemSearch.put("collectionId", contentCollection.id.toString)
          val out = new BasicDBObject()
          out.put("title", 1)
          Item.find(itemSearch, out) match {
            case cursor: SalatMongoCursor[Item] => {
              Ok(Json.toJson(cursor.toList))
            }
            case _ => NotFound
          }
        }
        case _ => NotFound
      }
  }
}
