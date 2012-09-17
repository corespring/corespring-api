package models

import collection.mutable
import com.mongodb.casbah.Imports._
import controllers.{QueryParser, LogType, InternalError}
import com.novus.salat.dao.ModelCompanion


trait Identifiable{
  var id:ObjectId;
}
/**
 * Created with IntelliJ IDEA.
 * User: josh
 * Date: 8/27/12
 * Time: 1:48 PM
 * To change this template use File | Settings | File Templates.
 */
trait Queryable[T <: AnyRef]{
  /**
   * the attributes of the queryable object, each wrapped in a QueryField
   */
  val queryFields:Seq[QueryField[T]];
  def preParse(dbo:DBObject):QueryParser = QueryParser()
}
trait DBQueryable[ObjectType <: Identifiable] extends ModelCompanion[ObjectType,ObjectId] with Queryable[ObjectType]

/**
 * used for wrapping the attributes of a queryable object
 * @param key - the string representation of the attribute. this is comparable to json and database keys
 * @param value -  a function that takes in a concrete instantiation of the Queryable type T and returns the value of the attribute specified by key
 * @param isValueValid - a function that takes in either the value of the attribute itself or a concrete instantiation of the Queryable type T
 *                     and returns a boolean specifying if the value is an acceptable value given the key
 *                     for example:
 *                        val grade = "52"
 *                        item.grade = "52"
 *                        val validGrade = "9"
 *                        queryField.isValueValid(grade) //returns false
 *                        queryField.isValueValid(item) //returns false
 *                        queryField.isValueValid(validGrade) //returns true
 *
 * @tparam T - a Queryable object
 */
trait QueryField[T <: AnyRef]{
  val key:String;
  val value:(T) => AnyRef;
  val isValueValid: ((String,AnyRef)) => Either[InternalError,(String,Any)]
  /**
   * used for comparing with key-value pair that may be using dot notation for embedded objects. for example
   * @param k - key of key-value pair
   * @return
   */
  def canHandleField(k:String):Boolean = key == k
}
case class QueryFieldString[T <: AnyRef](val key:String, val value:(T) => AnyRef, val valuefunc:(Any) => Either[InternalError,Any] = (v:Any) => Right(v)) extends QueryField[T]{
  val isValueValid = (kv:(String,AnyRef)) => QueryParser.isStringValueValid((kv._1,kv._2))(valuefunc)
}
case class QueryFieldStringArray[T <: AnyRef](val key:String, val value:(T) => AnyRef, val valuefunc:(Any) => Either[InternalError,Any] = (v:Any) => Right(v)) extends QueryField[T]{
  val isValueValid = (kv:(String,AnyRef)) => QueryParser.isStringArrayValueValid((kv._1,kv._2))(valuefunc)
}
case class QueryFieldNumber[T <: AnyRef](val key:String, val value:(T) => AnyRef, val valuefunc:(Any) => Either[InternalError,Any] = (v:Any) => Right(v)) extends QueryField[T]{
  val isValueValid = (kv:(String,AnyRef)) => QueryParser.isNumberValueValid((kv._1,kv._2))(valuefunc)
}
case class QueryFieldObject[T <: AnyRef](val key:String, val value:(T) => AnyRef, val valuefunc:(Any) => Either[InternalError,Any] = (v:Any) => Right(v),innerQueryFields:Seq[QueryField[_]] = Seq()) extends QueryField[T]{
  val isValueValid = (kv:(String,AnyRef)) =>
    if(kv._1 == "id") QueryParser.isObjectValueValid(("_id",kv._1))(valuefunc)
    else if(kv._1.contains(".")) innerQueryFields.find(qf => kv._1.substring(kv._1.indexOf(".")+1) == qf.key) match {
      case Some(qf) => qf.isValueValid(kv._1,kv._2)
      case None => Left(InternalError("embedded key "+kv._1+" is invalid",LogType.printError,true))
    }
    else QueryParser.isObjectValueValid((kv._1,kv._2))(valuefunc)
  override def canHandleField(k:String) =
    (key == k) || innerQueryFields.exists(qf =>{
      k == key+"."+qf.key
    })
}
case class QueryFieldObjectArray[T <: AnyRef](val key:String, val value:(T) => AnyRef, val valuefunc:(Any) => Either[InternalError,Any] = (v:Any) => Right(v),innerQueryFields:Seq[QueryField[_]] = Seq()) extends QueryField[T]{
  val isValueValid = (kv:(String,AnyRef)) =>
    if(kv._1.contains(".")) innerQueryFields.find(qf => kv._1.substring(kv._1.indexOf(".")+1) == qf.key) match {
      case Some(qf) => qf.isValueValid(kv._1,kv._2)
      case None => Left(InternalError("embedded key "+kv._1+" is invalid",LogType.printError,true))
    }
    else QueryParser.isObjectValueValid((kv._1,kv._2))(valuefunc)
  override def canHandleField(k:String) =
    (key == k) || innerQueryFields.exists(qf =>{
      k == key+"."+qf.key
    })
}
object QueryField{
  val valuefuncid:(Any) => Either[InternalError,Any] = (v:Any) => v match {
    case x:String => try{Right(new ObjectId(x))}catch{case e:IllegalArgumentException => Left(InternalError("value not a valid object id",addMessageToClientOutput = true))}
    case x:ObjectId => Right(x)
    case _ => Left(InternalError("value not a valid object id", addMessageToClientOutput = true))
  }
}
