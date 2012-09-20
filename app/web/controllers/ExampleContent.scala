package web.controllers

import org.bson.types.ObjectId
import play.api.mvc.{Action, Controller}
import models.{Item, ContentCollection, Content}

import com.mongodb.casbah.commons.MongoDBObject
import api.QueryHelper
import com.mongodb.casbah.commons.ValidBSONType.BasicDBObject
import com.mongodb.BasicDBObject
import play.api.libs.json.Json
import com.novus.salat.dao.SalatMongoCursor

object ExampleContent extends Controller {

  def index = Action {
    request =>
      Ok(web.views.html.exampleContent())
  }

  def items = Action {
    request =>

      val search = new BasicDBObject()
      search.put("name", "Beta Items")
      ContentCollection.findOne(search) match {
        case Some(contentCollection) => {
          println("contentCollection: " + contentCollection)
          println("contentCollection: " + contentCollection.id.toString)
          val itemSearch = new BasicDBObject()

          itemSearch.put("collectionId", contentCollection.id.toString)
          val out = new BasicDBObject()
          out.put("title", 1)
          out.put("collectionId", 1)

          Item.find(itemSearch, out) match {

            case cursor: SalatMongoCursor[Item] => {
              println(cursor)
              Ok(Json.toJson(cursor.toList))
            }
            case _ => NotFound
          }
        }
        case _ => NotFound
      }
  }
}
