package models.search

import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.Imports._
import com.mongodb.util.{JSONParseException, JSON}
import models._
import scala.Left
import controllers.InternalError
import scala.Some
import scala.Right


object CollectionSearch extends Searchable{
  def toSearchObj(query: AnyRef, optInitSearch: Option[MongoDBObject]):Either[SearchCancelled,MongoDBObject] = {
    def formatStringQuery(key:String, value:AnyRef, searchobj:MongoDBObject):Either[SearchCancelled,MongoDBObject] = {
      value match {
        case strval:String => Right(searchobj += key -> value)
        case dbobj:BasicDBObject => formatSpecOp(dbobj) match {
          case Right(newvalue) => Right(searchobj += key -> newvalue)
        }
        case _ => Left(SearchCancelled(Some(InternalError("invalid value when parsing search for "+key))))
      }
    }
    query match {
      case strquery:String => try{
        toSearchObj(JSON.parse(strquery),optInitSearch)
      }catch {
        case e:JSONParseException => Left(SearchCancelled(Some(InternalError(e.getMessage,clientOutput = Some("could not parse search string")))))
      }
      case dbquery:BasicDBObject => {
        dbquery.foldRight[Either[SearchCancelled,MongoDBObject]](Right(MongoDBObject()))((field,result) => result match {
            case Right(searchobj) => field._1 match {
                case ContentCollection.name => formatStringQuery(ContentCollection.name,field._2,searchobj)
                case _ => Left(SearchCancelled(Some(InternalError("uknown key when search on collections",addMessageToClientOutput = true))))
              }
            case Left(sc) => Left(sc)
          }) match {
          case Right(searchobj) => optInitSearch match {
            case Some(initSearch) => Right(searchobj ++ initSearch.asDBObject)
            case None => Right(searchobj)
          }
          case Left(sc) => Left(sc)
        }
      }
    }
  }

  def toFieldsObj(fields: AnyRef):Either[InternalError,SearchFields] = {
    fields match {
      case strfields:String => try{
        toFieldsObj(JSON.parse(strfields))
      }catch{
        case e:JSONParseException => Left(InternalError(e.getMessage,clientOutput = Some("could not parse search string")))
      }
      case dbfields:BasicDBObject => {
        val method:Int = if(dbfields.values().iterator().next() == 1) 1 else 0
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
            case Right(searchFields) => field._1 match {
              case ContentCollection.name => toSearchFieldObj(searchFields,field)
              case _ => Left(InternalError("unknown key for fields"))
            }
            case Left(error) => Left(error)
          }
        })
      }
    }
  }

  def toSortObj(field: AnyRef):Either[InternalError,MongoDBObject] = {
    def formatSortField(key:String,value:AnyRef):Either[InternalError,MongoDBObject] = {
      value match {
        case intval:java.lang.Integer => Right(MongoDBObject(key -> value))
        case _ => Left(InternalError("sort value not a number",addMessageToClientOutput = true))
      }
    }
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
          field._1 match {
            case ContentCollection.name => formatSortField(ContentCollection.name,field._2)
            case _ => Left(InternalError("unknown or invalid key contained in sort field",addMessageToClientOutput = true))
          }
        }
      }
    }
  }
}
