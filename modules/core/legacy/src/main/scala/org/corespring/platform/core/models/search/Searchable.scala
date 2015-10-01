package org.corespring.platform.core.models.search

import com.mongodb.casbah.Imports._
import com.mongodb.util.{ JSONParseException, JSON }
import java.util.regex.Pattern
import org.corespring.models.error.CorespringInternalError
import play.api.libs.json.{ Json, JsObject }

trait Searchable {
  protected val searchableFields: Seq[String] = Seq()

  final def toFieldsObj(fields: AnyRef): Either[CorespringInternalError, SearchFields] = {
    fields match {
      case strfields: String => try {
        toFieldsObj(JSON.parse(strfields))
      } catch {
        case e: JSONParseException => Left(CorespringInternalError(e.getMessage + "\ncould not parse search string"))
      }
      case dbfields: BasicDBObject => {
        val method: Int = if (dbfields.values().iterator().next() == 1) 1 else 0
        toFieldsObjInternal(dbfields, method)
      }
    }
  }
  protected def toFieldsObjInternal(dbfields: BasicDBObject, method: Int): Either[CorespringInternalError, SearchFields] = {
    if (searchableFields.isEmpty) throw new RuntimeException("when using default fields method, you must override searchable fields")
    def toSearchFieldObj(searchFields: SearchFields, field: (String, AnyRef), addToFieldsObj: Boolean = true, dbkey: String = ""): Either[CorespringInternalError, SearchFields] = {
      if (field._2 == method) {
        if (addToFieldsObj) {
          if (dbkey.isEmpty) field._1 else dbkey
          searchFields.dbfields = searchFields.dbfields += ((if (dbkey.isEmpty) field._1 else dbkey) -> field._2)
        }
        searchFields.jsfields = searchFields.jsfields :+ field._1
        Right(searchFields)
      } else {
        Left(CorespringInternalError("Wrong value for " + field._1 + ". Should have been " + method))
      }
    }
    dbfields.foldRight[Either[CorespringInternalError, SearchFields]](Right(SearchFields(method = method)))((field, result) => {
      result match {
        case Right(searchFields) => if (searchableFields.contains(field._1)) toSearchFieldObj(searchFields, field)
        else Left(CorespringInternalError("unknown field: " + field._1))
        case Left(error) => Left(error)
      }
    })
  }

  final def toSortObj(field: AnyRef): Either[CorespringInternalError, DBObject] = {
    field match {
      case strfield: String => try {
        val parsedobj: BasicDBObject = JSON.parse(strfield).asInstanceOf[BasicDBObject]
        toSortObj(parsedobj)
      } catch {
        case e: JSONParseException => Left(CorespringInternalError(e.getMessage + "\ncould not parse sort string"))
      }
      case dbfield: BasicDBObject => {
        if (dbfield.toSeq.size != 1) {
          Left(CorespringInternalError("cannot sort on multiple fields"))
        } else {
          val field = dbfield.toSeq.head
          toSortObjInternal(field)
        }
      }
    }
  }
  protected def toSortObjInternal(field: (String, AnyRef)): Either[CorespringInternalError, DBObject] = {
    if (searchableFields.isEmpty) throw new RuntimeException("when using default sort method, you must override searchable fields")
    def formatSortField(key: String, value: AnyRef): Either[CorespringInternalError, DBObject] = {
      value match {
        case intval: java.lang.Integer => Right(DBObject(key -> value))
        case _ => Left(CorespringInternalError("sort value not a number"))
      }
    }
    if (searchableFields.contains(field._1)) formatSortField(field._1, field._2)
    else Left(CorespringInternalError("invalid sort key: " + field._1))
  }
  final def toSearchObj(query: AnyRef, optInitSearch: Option[DBObject] = None, parseFields: Map[String, (AnyRef) => Either[CorespringInternalError, AnyRef]] = Map()): Either[SearchCancelled, DBObject] = {
    query match {
      case strquery: String => {
        val parsedobjResult: Either[SearchCancelled, BasicDBObject] = try {
          Right(JSON.parse(strquery).asInstanceOf[BasicDBObject])
        } catch {
          case e: JSONParseException => Left(SearchCancelled(Some(CorespringInternalError(s"could not parse search string: ${e.getMessage}"))))
          case e: ClassCastException => Left(SearchCancelled(Some(CorespringInternalError(s"could not parse search string: ${e.getMessage}"))))
        }
        parsedobjResult match {
          case Right(parsedobj) => toSearchObj(parsedobj, optInitSearch, parseFields)
          case Left(sc) => Left(sc)
        }
      }
      case dbquery: BasicDBObject => {
        if (dbquery.contains("$or")) {
          dbquery.get("$or") match {
            case dblist: BasicDBList => dblist.foldRight[Either[SearchCancelled, MongoDBList]](Right(MongoDBList()))((orcase, result) => {
              result match {
                case Right(dblist) => orcase match {
                  case dbobj: BasicDBObject => toSearchObjInternal(dbobj, None)(parseFields) match {
                    case Right(searchobj) => Right(dblist += searchobj)
                    case Left(sc) => sc.error match {
                      case None => Right(dblist)
                      case Some(_) => Left(sc)
                    }
                  }
                  case _ => Left(SearchCancelled(Some(CorespringInternalError("element within the array of or cases was not a db object"))))
                }
                case Left(sc) => Left(sc)
              }
            }) match {
              case Right(newlist) => toSearchObj(dbquery.filter(_._1 != "$or").asDBObject, optInitSearch, parseFields) match {
                case Right(remainder) => Right(remainder ++ DBObject("$or" -> newlist))
                case Left(sc) => Left(sc)
              }
              case Left(sc) => Left(sc)
            }
            case _ => Left(SearchCancelled(Some(CorespringInternalError("$or operator did not contain a list of documents for its value"))))
          }
        } else if (dbquery.contains("$and")) {
          dbquery.get("$and") match {
            case dblist: BasicDBList => dblist.foldRight[Either[SearchCancelled, MongoDBList]](Right(MongoDBList()))((andcase, result) => {
              result match {
                case Right(dblist) => andcase match {
                  case dbobj: BasicDBObject => toSearchObjInternal(dbobj, None)(parseFields) match {
                    case Right(searchobj) => Right(dblist += searchobj)
                    case Left(sc) => Left(sc)
                  }
                  case _ => Left(SearchCancelled(Some(CorespringInternalError("element within the array of or cases was not a db object"))))
                }
                case Left(sc) => Left(sc)
              }
            }) match {
              case Right(newlist) => toSearchObj(dbquery.filter(_._1 != "$and").asDBObject, optInitSearch, parseFields) match {
                case Right(remainder) => Right(remainder ++ DBObject("$and" -> newlist))
                case Left(sc) => Left(sc)
              }
              case Left(sc) => Left(sc)
            }
            case _ => Left(SearchCancelled(Some(CorespringInternalError("$or operator did not contain a list of documents for its value"))))
          }
        } else toSearchObjInternal(dbquery, optInitSearch)(parseFields)
      }
      case _ => Left(SearchCancelled(Some(CorespringInternalError("invalid search object"))))
    }
  }
  protected def toSearchObjInternal(dbquery: BasicDBObject, optInitSearch: Option[DBObject])(implicit parseFields: Map[String, (AnyRef) => Either[CorespringInternalError, AnyRef]]): Either[SearchCancelled, DBObject] = {
    if (searchableFields.isEmpty) throw new RuntimeException("when using default search method, you must override searchable fields")
    dbquery.foldRight[Either[SearchCancelled, DBObject]](Right(DBObject()))((field, result) => {
      result match {
        case Right(searchobj) => if (searchableFields.contains(field._1)) formatQuery(field._1, field._2, searchobj)
        else Left(SearchCancelled(Some(CorespringInternalError("unknown query field: " + field._1))))
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

  protected final def formatQuery(key: String, value: AnyRef, searchobj: DBObject)(implicit parseFields: Map[String, (AnyRef) => Either[CorespringInternalError, AnyRef]]): Either[SearchCancelled, DBObject] = {
    parseFields.find(_._1 == key) match {
      case Some(parseField) => parseField._2(value) match {
        case Right(newvalue) => Right(searchobj += key -> newvalue)
        case Left(error) => Left(SearchCancelled(Some(error)))
      }
      case None => value match {
        case value if value.isInstanceOf[String] || value.isInstanceOf[Boolean] || value.isInstanceOf[Pattern] || value.isInstanceOf[java.lang.Integer] => Right(searchobj += key -> value)
        case dbobj: BasicDBObject => formatSpecOp(dbobj) match {
          case Right(newvalue) => Right(searchobj += key -> newvalue)
          case Left(e) => throw new RuntimeException(e.message)
        }
        case _ => Left(SearchCancelled(Some(CorespringInternalError("invalid value when parsing search for " + key))))
      }
    }
  }

  protected final def formatSpecOp(dbobj: BasicDBObject): Either[CorespringInternalError, AnyRef] = {
    dbobj.toSeq.headOption match {
      case Some((key, value)) => key match {
        case "$in" => if (value.isInstanceOf[BasicDBList]) Right(DBObject(key -> value))
        else Left(CorespringInternalError("$in did not contain an array of elements"))
        case "$nin" => if (value.isInstanceOf[BasicDBList]) Right(DBObject(key -> value))
        else Left(CorespringInternalError("$nin did not contain an array of elements"))
        case "$exists" => if (value.isInstanceOf[Boolean]) Right(DBObject(key -> value))
        else Left(CorespringInternalError("$exists did not contain a boolean value"))
        case "$ne" => Right(DBObject(key -> value))
        case "$all" => if (value.isInstanceOf[BasicDBList]) Right(DBObject(key -> value))
        else Left(CorespringInternalError("$all did not contain an array of elements"))
        case _ => if (key.startsWith("$")) Left(CorespringInternalError("unsupported special operation"))
        else Left(CorespringInternalError("cannot have embedded db object without special operator"))
      }
      case None => Left(CorespringInternalError("cannot have empty embedded db object as value"))
    }
  }
}

case class SearchCancelled(error: Option[CorespringInternalError])

/**
 *
 * @param dbfields
 * @param jsfields
 */
//TODO - remove mutability
case class SearchFields(var dbfields: DBObject = DBObject(), var jsfields: Seq[String] = Seq(), method: Int) {
  val inclusion = method == 1
  val exclusion = method == 0

  /**
   * Ensure that playerDefinition and data are returned so the item format can be derived.
   * @return
   */
  def fieldsToReturn = {
    dbfields.put("playerDefinition", 1)
    dbfields.put("data", 1)
    dbfields
  }

  /**
   * Ensure that "format" is always added and never removed
   * @param json
   * @return
   */
  def processJson(json: JsObject): JsObject = {

    def addToJson(key: String, acc: JsObject): JsObject = if ((Seq("format", "id") ++ jsfields).contains(key)) {
      acc ++ Json.obj(key -> (json \ key))
    } else {
      acc
    }

    def removeFromJson(key: String, acc: JsObject): JsObject = if ((jsfields diff "format").contains(key)) {
      acc - key
    } else {
      acc
    }

    if (inclusion) {
      json.keys.foldRight(Json.obj())(addToJson)
    } else {
      json.keys.foldRight(json)(removeFromJson)
    }
  }

  def addDbFieldsToJsFields = {
    dbfields.foreach(field => if (!jsfields.contains(field._1)) jsfields = jsfields :+ field._1)
  }
}

