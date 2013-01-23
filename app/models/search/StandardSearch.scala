package models.search

import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.Imports._
import scala.Left
import scala.Right
import scala.Some
import controllers.InternalError
import com.mongodb.util.{JSONParseException, JSON}
import models.{Standard, Subject}

object StandardSearch extends Searchable{
  def toSearchObj(query: AnyRef, optInitSearch: Option[MongoDBObject]):Either[SearchCancelled,MongoDBObject] = {
    def formatStringQuery(field:(String,AnyRef), searchobj:MongoDBObject):Either[SearchCancelled,MongoDBObject] = {
      field._2 match {
        case strval:String => Right(searchobj += field)
        case dbobj:BasicDBObject => formatSpecOp(dbobj) match {
          case Right(newvalue) => Right(searchobj += field)
        }
        case _ => Left(SearchCancelled(Some(InternalError("invalid value when parsing search for "+field._1))))
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
            case Standard.DotNotation => formatStringQuery(field,searchobj)
            case Standard.Category => formatStringQuery(field,searchobj)
            case Standard.Subject => formatStringQuery(field,searchobj)
            case Standard.SubCategory => formatStringQuery(field,searchobj)
            case Standard.guid => formatStringQuery(field,searchobj)
            case Standard.Standard => formatStringQuery(field,searchobj)
            case _ => Left(SearchCancelled(Some(InternalError("uknown key when search on field values",addMessageToClientOutput = true))))
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
              case Standard.DotNotation => toSearchFieldObj(searchFields,field)
              case Standard.Category => toSearchFieldObj(searchFields,field)
              case Standard.Subject => toSearchFieldObj(searchFields,field)
              case Standard.SubCategory => toSearchFieldObj(searchFields,field)
              case Standard.guid => toSearchFieldObj(searchFields,field)
              case Standard.Standard => toSearchFieldObj(searchFields,field)
              case _ => Left(InternalError("unknown key for fields"))
            }
            case Left(error) => Left(error)
          }
        })
      }
    }
  }

  def toSortObj(field: AnyRef):Either[InternalError,MongoDBObject] = {
    def formatSortField(field:(String,AnyRef)):Either[InternalError,MongoDBObject] = {
      field._2 match {
        case intval:java.lang.Integer => Right(MongoDBObject(field))
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
            case Standard.DotNotation => formatSortField(field)
            case Standard.Category => formatSortField(field)
            case Standard.Subject => formatSortField(field)
            case Standard.SubCategory => formatSortField(field)
            case Standard.guid => formatSortField(field)
            case Standard.Standard => formatSortField(field)
            case _ => Left(InternalError("unknown or invalid key contained in sort field",addMessageToClientOutput = true))
          }
        }
      }
    }
  }
}
