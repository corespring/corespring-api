package models

import play.api.libs.json.JsValue
import com.mongodb.casbah.Imports._
import controllers.InternalError
import collection.mutable

/**
 * Created with IntelliJ IDEA.
 * User: josh
 * Date: 8/27/12
 * Time: 1:48 PM
 * To change this template use File | Settings | File Templates.
 */
trait Queryable {
  //val queryFields:Map[String,String];
 // def parseQuery(json:JsValue):Either[InternalError, DBObject];
}
class QueryField[T](val key:String, val keyType:String, val value:(T) => AnyRef)
object QueryField{
  def apply[T](key:String,keyType:String,value:(T) => AnyRef) = new QueryField[T](key,keyType,value)
  val ObjectType = "Object"
  val ObjectIdType = "ObjectId"
  val StringType = "String"
  val NumberType = "Number"
  val StringArrayType = "Seq[String]"
  val NumberArrayType = "Seq[Number]"
  val ObjectArrayType = "Seq[Object]"
  val ObjectIdArrayType = "Seq[ObjectId]"
}