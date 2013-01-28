package models.search

import com.mongodb.{BasicDBObject}
import com.mongodb.casbah.Imports._
import java.util.regex.Pattern
import controllers.{LogType, InternalError}
import scala.Left
import scala.Some
import scala.Right
import com.mongodb.util.{JSONParseException, JSON}

trait Searchable {
  protected val searchableFields:Seq[String] = Seq()

  final def toFieldsObj(fields: AnyRef):Either[InternalError,SearchFields] = {
    fields match {
      case strfields:String => try{
        toFieldsObj(JSON.parse(strfields))
      }catch{
        case e:JSONParseException => Left(InternalError(e.getMessage,clientOutput = Some("could not parse search string")))
      }
      case dbfields:BasicDBObject => {
        val method:Int = if(dbfields.values().iterator().next() == 1) 1 else 0
        toFieldsObjInternal(dbfields,method)
      }
    }
  }
  protected def toFieldsObjInternal(dbfields:BasicDBObject, method:Int):Either[InternalError,SearchFields] = {
    if (searchableFields.isEmpty) throw new RuntimeException("when using default fields method, you must override searchable fields")
    def toSearchFieldObj(searchFields:SearchFields,field:(String,AnyRef),addToFieldsObj:Boolean = true,dbkey:String=""):Either[InternalError,SearchFields] = {
      if(field._2 == method){
        if(addToFieldsObj) {
          if(dbkey.isEmpty) field._1 else dbkey
          searchFields.dbfields = searchFields.dbfields += ((if(dbkey.isEmpty) field._1 else dbkey) -> field._2)
        }
        searchFields.jsfields = searchFields.jsfields :+ field._1
        Right(searchFields)
      }else{
        Left(InternalError("Wrong value for "+field._1+". Should have been "+method,addMessageToClientOutput = true))
      }
    }
    dbfields.foldRight[Either[InternalError,SearchFields]](Right(SearchFields(method = method)))((field,result) => {
      result match {
        case Right(searchFields) => if (searchableFields.contains(field._1)) toSearchFieldObj(searchFields,field)
          else Left(InternalError("unknown field: "+field._1,addMessageToClientOutput = true))
        case Left(error) => Left(error)
      }
    })
  }

  final def toSortObj(field:AnyRef):Either[InternalError,MongoDBObject] = {
    field match {
      case strfield:String => try{
        val parsedobj:BasicDBObject = JSON.parse(strfield).asInstanceOf[BasicDBObject]
        toSortObj(parsedobj)
      }catch {
        case e:JSONParseException => Left(InternalError(e.getMessage,clientOutput = Some("could not parse sort string")))
      }
      case dbfield:BasicDBObject => {
        if (dbfield.toSeq.size != 1){
          Left(InternalError("cannot sort on multiple fields",addMessageToClientOutput = true))
        }else{
          val field = dbfield.toSeq.head
          toSortObjInternal(field)
        }
      }
    }
  }
  protected def toSortObjInternal(field:(String,AnyRef)):Either[InternalError,MongoDBObject] = {
    if (searchableFields.isEmpty) throw new RuntimeException("when using default sort method, you must override searchable fields")
    def formatSortField(key:String,value:AnyRef):Either[InternalError,MongoDBObject] = {
      value match {
        case intval:java.lang.Integer => Right(MongoDBObject(key -> value))
        case _ => Left(InternalError("sort value not a number",addMessageToClientOutput = true))
      }
    }
    if (searchableFields.contains(field._1)) formatSortField(field._1,field._2)
    else Left(InternalError("invalid sort key: "+field._1))
  }

  final def toSearchObj(query: AnyRef, optInitSearch:Option[MongoDBObject]): Either[SearchCancelled,MongoDBObject] = {
    query match {
      case strquery:String => try{
        val parsedobj:BasicDBObject = JSON.parse(strquery).asInstanceOf[BasicDBObject]
        toSearchObj(parsedobj,optInitSearch)
      }catch {
        case e:JSONParseException => Left(SearchCancelled(Some(InternalError(e.getMessage,clientOutput = Some("could not parse search string")))))
      }
      case dbquery:BasicDBObject => {
        if(dbquery.contains("$or")){
          dbquery.get("$or") match {
            case dblist:BasicDBList => dblist.foldRight[Either[SearchCancelled,MongoDBList]](Right(MongoDBList()))((orcase,result) => {
              result match {
                case Right(dblist) => orcase match {
                  case dbobj:BasicDBObject => toSearchObjInternal(dbobj,optInitSearch) match {
                    case Right(searchobj) => Right(dblist += searchobj)
                    case Left(sc) => Left(sc)
                  }
                  case _ => Left(SearchCancelled(Some(InternalError("element within the array of or cases was not a db object",addMessageToClientOutput = true))))
                }
                case Left(sc) => Left(sc)
              }
            }) match {
              case Right(newlist) => Right(MongoDBObject("$or" -> newlist))
              case Left(sc) => Left(sc)
            }
            case _ => Left(SearchCancelled(Some(InternalError("$or operator did not contain a list of documents for its value",addMessageToClientOutput = true))))
          }
        } else toSearchObjInternal(dbquery,optInitSearch)
      }
      case _ => Left(SearchCancelled(Some(InternalError("invalid search object",LogType.printFatal,addMessageToClientOutput = true))))
    }
  }
  protected def toSearchObjInternal(dbquery:BasicDBObject, optInitSearch:Option[MongoDBObject]):Either[SearchCancelled,MongoDBObject] = {
    if(searchableFields.isEmpty) throw new RuntimeException("when using default search method, you must override searchable fields")
    dbquery.foldRight[Either[SearchCancelled,MongoDBObject]](Right(MongoDBObject()))((field,result) => {
      result match {
        case Right(searchobj) => if (searchableFields.contains(field._1)) formatQuery(field._1,field._2,searchobj)
          else Left(SearchCancelled(Some(InternalError("unknown query field: "+field._1))))
        case Left(sc) => Left(sc)
      }
    }) match {
      case Right(searchobj) => optInitSearch match {
        case Some(initSearch) => Right(searchobj ++ initSearch.asDBObject)
        case None => Right(searchobj)
      }
      case Left(sc) => Left(sc)
    }
  }

  protected final def formatQuery(key:String,value:AnyRef,searchobj:MongoDBObject):Either[SearchCancelled,MongoDBObject] = {
    value match {
      case value if value.isInstanceOf[String] || value.isInstanceOf[Boolean] || value.isInstanceOf[Pattern]=> Right(searchobj += key -> value)
      case dbobj:BasicDBObject => formatSpecOp(dbobj) match {
        case Right(newvalue) => Right(searchobj += key -> newvalue)
      }
      case _ => Left(SearchCancelled(Some(InternalError("invalid value when parsing search for "+key))))
    }
  }

  protected final def formatSpecOp(dbobj:BasicDBObject):Either[InternalError,AnyRef] = {
    dbobj.toSeq.headOption match {
      case Some((key,value)) => key match {
        case "$in" => if(value.isInstanceOf[BasicDBList]) Right(MongoDBObject(key -> value))
          else Left(InternalError("$in did not contain an array of elements",addMessageToClientOutput = true))
        case "$nin" => if(value.isInstanceOf[BasicDBList]) Right(MongoDBObject(key -> value))
          else Left(InternalError("$nin did not contain an array of elements",addMessageToClientOutput = true))
        case "$exists" => if(value.isInstanceOf[Boolean]) Right(MongoDBObject(key -> value))
          else Left(InternalError("$exists did not contain a boolean value",addMessageToClientOutput = true))
        case "$ne" => Right(MongoDBObject(key -> value))
        case "$all" => if (value.isInstanceOf[BasicDBList]) Right(MongoDBObject(key -> value))
          else Left(InternalError("$all did not contain an array of elements",addMessageToClientOutput = true))
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

  def addDbFieldsToJsFields = {
    dbfields.foreach(field => if (!jsfields.contains(field._1)) jsfields = jsfields :+ field._1)
  }
}


