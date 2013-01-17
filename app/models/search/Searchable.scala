package models.search

import com.mongodb.{BasicDBObject, DBObject}
import com.mongodb.casbah.commons.MongoDBObject
import controllers.InternalError
import java.util.regex.Pattern
import org.bson.types.ObjectId

trait Searchable {
  def toSearchObj(query: AnyRef, initSearch:Option[MongoDBObject]): Either[SearchCancelled,MongoDBObject]
  def toFieldsObj(fields: AnyRef):Either[InternalError,SearchFields]
}
case class SearchCancelled(error:Option[InternalError])

/**
 *
 * @param dbfields
 * @param jsfields
 * @param inclusion
 */
case class SearchFields(var dbfields:MongoDBObject = MongoDBObject(), var jsfields:Seq[String] = Seq(), val inclusion:Boolean = false)


