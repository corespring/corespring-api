package models

import com.mongodb.{BasicDBObject, DBObject}
import com.mongodb.casbah.commons.MongoDBObject
import controllers.InternalError
import java.util.regex.Pattern
import org.bson.types.ObjectId

trait Searchable {
  def toSearchObj(query: AnyRef): Either[QueryCancelled,MongoDBObject]

  def formatStringQuery[T](key:String, value:AnyRef, searchobj:MongoDBObject):Either[InternalError,MongoDBObject] = {
    value match {
      case strval:String => Right(searchobj += key -> Pattern.compile(strval,Pattern.CASE_INSENSITIVE))
      case _ => Left(InternalError("invalid value when parsing search for "+key))
    }
  }
}
trait Identifiable{
  val id:ObjectId
}
case class QueryCancelled(error:Option[InternalError])
