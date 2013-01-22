package models.search

import com.mongodb.{BasicDBObject}
import com.mongodb.casbah.commons.MongoDBObject
import controllers.InternalError
import com.mongodb.casbah.Imports._
import java.util.regex.Pattern

trait Searchable {
  def toSearchObj(query: AnyRef, optInitSearch:Option[MongoDBObject]): Either[SearchCancelled,MongoDBObject]
  def toFieldsObj(fields: AnyRef):Either[InternalError,SearchFields]
  def toSortObj(field:AnyRef):Either[InternalError,MongoDBObject]
  def formatSpecOp(dbobj:BasicDBObject):Either[InternalError,AnyRef] = {
    dbobj.toSeq.headOption match {
      case Some((key,value)) => key match {
        case "$in" => if(value.isInstanceOf[BasicDBList]) Right(MongoDBObject(key -> value))
          else Left(InternalError("$in did not contain an array of elements",addMessageToClientOutput = true))
        case "$nin" => if(value.isInstanceOf[BasicDBList]) Right(MongoDBObject(key -> value))
          else Left(InternalError("$nin did not contain an array of elements",addMessageToClientOutput = true))
        case "$exists" => if(value.isInstanceOf[Boolean]) Right(MongoDBObject(key -> value))
          else Left(InternalError("$exists did not contain a boolean value",addMessageToClientOutput = true))
        case "$regex" => if (value.isInstanceOf[String]) Right(Pattern.compile(value.asInstanceOf[String],Pattern.CASE_INSENSITIVE))
          else Left(InternalError("$regex did not contain a string for pattern matching",addMessageToClientOutput = true))
        case "$ne" => Right(MongoDBObject(key -> value))
        case _ => if (key.startsWith("$")) Left(InternalError("unsupported special operation",addMessageToClientOutput = true))
          else Left(InternalError("cannot have embedded db object without special operator",addMessageToClientOutput = true))
      }
      case None => Left(InternalError("cannot have empty embedded db object as value",addMessageToClientOutput = true))
    }
  }
}
case class SearchCancelled(error:Option[InternalError])

/**
 *
 * @param dbfields
 * @param jsfields
 */
case class SearchFields(var dbfields:MongoDBObject = MongoDBObject(), var jsfields:Seq[String] = Seq(), method:Int){
  val inclusion = method == 1
  val exclusion = method == 0
}


