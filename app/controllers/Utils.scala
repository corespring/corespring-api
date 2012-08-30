package controllers

import com.novus.salat.dao.SalatMongoCursor
import collection.mutable
import com.mongodb.casbah.Imports._
import scala.Left
import com.novus.salat.dao.SalatMongoCursor
import scala.Right
import org.bson.types.ObjectId
import com.mongodb.util.JSON
import models.QueryField

/**
 * Created with IntelliJ IDEA.
 * User: josh
 * Date: 8/17/12
 * Time: 9:40 AM
 * To change this template use File | Settings | File Templates.
 */

object Utils {
  /**
   * return a sequence of object T's. closes the cursor after the sequence has been computed
   * @param c
   * @tparam T
   * @return
   */
  def toSeq[T <: AnyRef](c:SalatMongoCursor[T]):Seq[T] = {
    val seq = c.foldRight[Seq[T]](Seq())((o,acc) => acc :+ o)
    c.close()
    seq
  }
}
case class JsonValidationException(field:String) extends RuntimeException("invalid value for: "+field)
object QueryParser{

  def buildQuery(query:String):Either[InternalError, mutable.Builder[(String,Any),DBObject]] = {
    JSON.parse(query) match {
      case dbo:DBObject => try{
        var queryBuilder = MongoDBObject.newBuilder
        val fields:Iterator[(String,AnyRef)] = dbo.iterator
        Right(queryBuilder)
      }catch{
        case e:RuntimeException => Left(InternalError(e.getMessage,LogType.printError,true))
      }
      case _ => Left(InternalError("invalid format query object",LogType.printError,true))
    }
  }
  def parseFields(iter:Iterator[(String,AnyRef)], acc:Either[InternalError,mutable.Builder[(String,Any),DBObject]]):Either[InternalError,mutable.Builder[(String,Any),DBObject]] = {
    if (iter.hasNext && acc.isRight){
      val field = iter.next()

    }else acc
    acc
  }
  def parseValue(key:String, keyType:String, value:Any, acc:mutable.Builder[(String,Any),DBObject]):Either[InternalError, mutable.Builder[(String,Any),DBObject]] = {
    def defaultReturn:Either[InternalError,mutable.Builder[(String,Any),DBObject]] = Left(InternalError("invalid value "+value.toString+" for "+key,LogType.printError,true))
    def embeddedObjectBuilder(dbo:DBObject):Either[InternalError, mutable.Builder[(String,Any),DBObject]] = parseEmbeddedFields(keyType, dbo.iterator, MongoDBObject.newBuilder) match {
      case Right(builder) => Right(acc += (key -> builder.result()))
      case Left(error) =>Left(error)
    }
    keyType match {
      case QueryField.ObjectType | QueryField.ObjectArrayType => value match {
        case dbo:DBObject => embeddedObjectBuilder(dbo)
        case _ => defaultReturn
      }
      case QueryField.ObjectIdType | QueryField.ObjectIdArrayType => value match {
        case dbo:DBObject => embeddedObjectBuilder(dbo)
        case oid:String => try{
          Right(acc += (key -> new ObjectId(oid)))
        }catch{
          case e:IllegalArgumentException => Left(InternalError("value "+oid+" is not a valid object id",LogType.printError,true))
        }
        case _ => defaultReturn
      }
      case QueryField.StringType | QueryField.StringArrayType => value match {
        case v:String => Right(acc += (key -> v))
        case dbo:DBObject => embeddedObjectBuilder(dbo)
        case _ => defaultReturn
      }
      case QueryField.NumberType | QueryField.NumberArrayType => value match {
        case v:Number => Right(acc += (key -> v.intValue()))
        case dbo:DBObject => embeddedObjectBuilder(dbo)
        case _ => defaultReturn
      }
    }
  }
  def parseEmbeddedFields(keyType:String, fields:Iterator[(String,AnyRef)], acc:mutable.Builder[(String,Any),DBObject]):Either[InternalError, mutable.Builder[(String,Any),DBObject]] = {
    if (fields.hasNext){
      parseEmbeddedField(keyType,fields.next(),acc) match {
        case Right(builder) => parseEmbeddedFields(keyType,fields,acc)
        case Left(error) => Left(error)
      }
    }else Right(acc)
  }
  private def parseEmbeddedField(keyType:String, field:(String,AnyRef), acc:mutable.Builder[(String,Any),DBObject]):Either[InternalError, mutable.Builder[(String,Any),DBObject]] = {
    keyType match {
      case QueryField.ObjectType => field._1 match {
        case "$ne" => field._2 match {
          case x:DBObject => if (x.find(xfield => xfield._1.contains("$")).isEmpty){
            Right(acc += (field._1 -> field._2))
          } else Left(InternalError(field._2+" cannot contain special characters (e.g. $)"))
          case _ => Left(InternalError(field._2+" is not a db object"))
        }
        case "$in" | "$nin" => field._2 match {
          case x:MongoDBList => if (x.find(xfield => !xfield.isInstanceOf[DBObject]).isEmpty){
            if(x.find(xfield => xfield.asInstanceOf[DBObject].find(v => v._1.contains("$")).isDefined).isEmpty){
              Right(acc += (field._1 -> field._2))
            }else Left(InternalError("one or more objects in the given array contain special characters",LogType.printError,true))
          }else Left(InternalError("one or more elements in the array was not a db object",LogType.printError,true))
          case _ => Left(InternalError("the given value is not a list: "+field._2.toString,LogType.printError,true))
        }
        case x if x startsWith "$" => Left(InternalError("invalid special character given"))
        case _ => Right(acc += (field._1 -> field._2))
      }
      case QueryField.ObjectIdType => field._1 match {
        case "$ne" => field._2 match {
          case x:String => try{
            Right(acc += ("$ne" -> new ObjectId(x)))
          } catch{
            case e:IllegalArgumentException => Left(InternalError("{$ne:"+x+"} : "+x+" is not an object id",LogType.printError,true))
          }
          case _ => Left(InternalError("{$ne:"+field._2.toString()+"} : "+field._2.toString()+" is not a string",LogType.printError,true))
        }
        case "$in" | "$nin" => field._2 match{
          case x:MongoDBList => try{
            Right(acc += (field._1 -> x.map(id => new ObjectId(id.asInstanceOf[String]))))
          }catch{
            case e:IllegalArgumentException => Left(InternalError("{"+field._1+":"+x.toString()+"} : "+x.toString()+" not object id(s)",LogType.printError,true))
            case e:ClassCastException=> Left(InternalError("{"+field._1+":"+x.toString()+"} : "+x.toString()+" is not a string",LogType.printError,true))
          }
          case _ => Left(InternalError("{"+field._1+":"+field._2.toString()+"} : "+field._2.toString()+" is not an array",LogType.printError,true))
        }
        case _ => Left(InternalError("unsupported modifier "+field._1))
      }
      case QueryField.StringType => field._1 match {
        case "$ne" => field._2 match {
          case x:String => Right(acc += ("$ne" -> x))
          case _ => Left(InternalError("{$ne:"+field._2.toString()+"} : "+field._2.toString()+"value is not a string",LogType.printError,true))
        }
        case "$in" | "$nin" => field._2 match {
          case x:MongoDBList => try{
            Right(acc += (field._1 -> x.map(id => id.asInstanceOf[String])))
          }catch{
            case e:ClassCastException => Left(InternalError("{"+field._1+":"+x.toString()+"} : "+x.toString()+" is not all strings",LogType.printError,true))
          }
          case _ => Left(InternalError("{"+field._1+":"+field._2.toString()+"} : "+field._2.toString()+" is not an array",LogType.printError,true))
        }
        case _ => Left(InternalError("unsupported modifier "+field._1))
      }
      case QueryField.NumberType => field._1 match {
        case "$gt" | "$lt" | "$gte" | "$lte" | "$ne" => field._2 match {
          case x: Number => Right(acc += (field._1 -> x.intValue()))
          case _ => Left(InternalError("{"+field._1+":"+field._2.toString+"} : "+field._2.toString()+" not a number"))
        }
        case "$nin" | "$in" => field._2 match {
          case x:MongoDBList => try{
            Right(acc += (field._1 -> x.map(id => id.asInstanceOf[Int])))
          }catch{
            case e:ClassCastException => Left(InternalError("{"+field._1+":"+x.toString()+"} : "+x.toString()+" is not all numbers",LogType.printError,true))
          }
          case _ => Left(InternalError("{"+field._1+":"+field._2.toString()+"} : "+field._2.toString()+" is not an array",LogType.printError,true))
        }
        case _ => Left(InternalError("unsupported modifier "+field._1))
      }
      case QueryField.StringArrayType => field._1 match{
        case "$all" | "$nin" | "$in" => field._2 match {
          case x:MongoDBList => try{
            Right(acc += (field._1 -> x.map(xfield => xfield.asInstanceOf[String])))
          }catch{
            case e:ClassCastException => Left(InternalError("{"+field._1+":"+x.toString()+"} : "+x.toString()+" is not all strings",LogType.printError,true))
          }
          case _ => Left(InternalError("{"+field._1+":"+field._2.toString()+"} : "+field._2.toString()+" is not an array",LogType.printError,true))
        }
        case "$size" => field._2 match {
          case x:Number => Right(acc += (field._1 -> x.intValue()))
          case _ => Left(InternalError("{"+field._1+":"+field._2.toString()+"} : "+field._2.toString()+" is not a number",LogType.printError,true))
        }
        case _ => Left(InternalError("unsupported modifier "+field._1))
      }
      case QueryField.NumberArrayType => field._1 match {
        case "$gt" | "$lt" | "$lte" | "$gte" => field._2 match {
          case x: Number => Right(acc += (field._1 -> x.intValue()))
          case _ => Left(InternalError("{"+field._1+":"+field._2.toString+"} : "+field._2.toString()+" not a number"))
        }
        case "$all" | "$in" | "$nin" => field._2 match {
          case x:MongoDBList => try{
            Right(acc += (field._1 -> x.map(_.asInstanceOf[Int])))
          }catch{
            case e:ClassCastException => Left(InternalError("{"+field._1+":"+x.toString()+"} : "+x.toString()+" is not all strings",LogType.printError,true))
          }
        }
        case "$size" => field._2 match {
          case x:Number => Right(acc += (field._1 -> x.intValue()))
          case _ => Left(InternalError("{"+field._1+":"+field._2.toString()+"} : "+field._2.toString()+" is not a number",LogType.printError,true))
        }
        case "$elemMatch" => field._2 match {
          case x:DBObject => Right(acc += (field._1 -> field._2))
          case _ => Left(InternalError("the value given for $elemMatch was not an object"))
        }
      }
      case QueryField.ObjectArrayType => field._1 match {
        case "$in" | "$nin" => field._2 match {
          case x:MongoDBList => if (x.find(xfield => !xfield.isInstanceOf[DBObject]).isEmpty){
            if(x.find(xfield => xfield.asInstanceOf[DBObject].find(v => v._1.contains("$")).isDefined).isEmpty){
              Right(acc += (field._1 -> field._2))
            }else Left(InternalError("one or more objects in the given array contain special characters",LogType.printError,true))
          }else Left(InternalError("one or more elements in the array was not a db object",LogType.printError,true))
          case _ => Left(InternalError("the given value is not a list: "+field._2.toString,LogType.printError,true))
        }
        case "$size" => field._2 match {
          case x:Number => Right(acc += (field._1 -> x.intValue()))
          case _ => Left(InternalError("{"+field._1+":"+field._2.toString()+"} : "+field._2.toString()+" is not a number",LogType.printError,true))
        }
        case "$elemMatch" => field._2 match {
          case x:DBObject => Right(acc += (field._1 -> field._2))
          case _ => Left(InternalError("the value given for $elemMatch was not an object"))
        }
      }
      case QueryField.ObjectIdArrayType => field._1 match {
        case "$all"| "$in" | "$nin" => field._2 match{
          case x:MongoDBList => try{
            Right(acc += (field._1 -> x.map(id => new ObjectId(id.asInstanceOf[String]))))
          }catch{
            case e:IllegalArgumentException => Left(InternalError("{"+field._1+":"+x.toString()+"} : "+x.toString()+" not object id(s)",LogType.printError,true))
            case e:ClassCastException=> Left(InternalError("{"+field._1+":"+x.toString()+"} : "+x.toString()+" is not a string",LogType.printError,true))
          }
          case _ => Left(InternalError("{"+field._1+":"+field._2.toString()+"} : "+field._2.toString()+" is not an array",LogType.printError,true))
        }
        case "$size" => field._2 match {
          case x:Number => Right(acc += (field._1 -> x.intValue()))
          case _ => Left(InternalError("{"+field._1+":"+field._2.toString()+"} : "+field._2.toString()+" is not a number",LogType.printError,true))
        }
      }
      case _ => throw new RuntimeException("you passed an unknown key type. this is the developer's fault")
    }
  }
}
