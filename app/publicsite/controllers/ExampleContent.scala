package publicsite.controllers

import play.api.mvc.{Action, Controller}
import models.{Item, ContentCollection, Content}

import com.mongodb.BasicDBObject

object ExampleContent extends Controller {

  val EXAMPLE_CONTENT_COLLECTION_NAME = "Beta Items"

  def items(q: Option[String]) = Action {
    request =>
      val search = new BasicDBObject()
      search.put("name", EXAMPLE_CONTENT_COLLECTION_NAME)
      ContentCollection.findOne(search) match {
        case Some(contentCollection) => {
//          QueryHelper.listSimple(Item,
//            q,
//            Some(MongoDBObject("title" -> 1, "itemType" -> 1, "subjects" -> 1, "gradeLevel" -> 1, "standards" -> 1, "contributorDetails" -> 1)),
//            false,
//            initSearch = Some(MongoDBObject("collectionId" -> contentCollection.id.toString)))
          //TODO: re-implement
          Ok
        }
        case _ => NotFound
      }
  }
}
