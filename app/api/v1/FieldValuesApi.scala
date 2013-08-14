package api.v1

import fieldValues.{Options, QueryOptions}
import play.api.libs.json.Json._
import play.api.mvc.{Result, Action}
import play.api.Play.current
import play.api.libs.json
import play.api.libs.json._
import org.corespring.platform.core.models._
import controllers.auth.{BaseApi}
import com.mongodb.casbah.Imports._
import scala.Some
import play.api.cache.Cache

import api.ApiError
import com.mongodb.casbah.commons.MongoDBObject
import org.corespring.platform.core.models.{Subject, Standard}
import org.corespring.platform.core.models.search.SearchCancelled
import org.corespring.platform.core.models.item.FieldValue

object FieldValuesApi extends BaseApi {

  val FieldValueCacheKey = "fieldValue"

  val AllAvailable = buildAllAvailable

  def buildAllAvailable = {
    val list = FieldValue.descriptions.toList :::
      List(
        ("cc-standard", Standard.description + " (list queries available)"),
        ("subject", Subject.description + " (list queries available)")
      )
    for {d <- list} yield Map("path" -> ("/api/v1/field_values/" + d._1), "description" -> d._2)
  }

  def getAllAvailable = Action {
    request =>
      Ok(toJson(AllAvailable))
  }

  /**
   * We store subject and standard in their own collection - so they are query-able.
   * The others exist in a single document in the fieldValues collection.
   * @param fieldName
   * @param q
   * @param f
   * @param c
   * @param sk
   * @param l
   * @return
   */
  def getFieldValues(fieldName: String, q: Option[String], f: Option[String], c: String, sk: Int, l: Int) = Action {
    request =>
      val jsValue = getFieldValuesAsJsValue(fieldName,q,f,c,sk,l)
      Ok(toJson(jsValue))
  }

  /**
   * @param fieldOptions -  a map of options for each field, will be extracted by [[api.v1.fieldValues.QueryOptions]]
   * eg:
   * {{{
   *  { "subject" : { q: {}, f: {}, l: 1, sk: 1} , "standards" : {...}}
   * }}}
   */
  def multiple(fieldNames: String, fieldOptions: Option[String], c: String ) = Action {
    val names: Seq[String] = fieldNames.split(",").toSeq

    def _getItems(names: Seq[String]): Map[String, JsValue] = names match {
      case Nil => Map()
      case _ => {
        val n: String = names.head
        val options : Options = getOptionsForField(n, fieldOptions)
        val value: JsValue = getFieldValuesAsJsValue(n, options.query, options.filter, c, options.skip, options.limit)
        Map((n -> value)) ++ _getItems(names.tail)
      }
    }
    val items = _getItems(names)
    Ok(toJson(items))
  }

  /**
   * Extract the values from the json string
   */
  private def getOptionsForField(name:String, options : Option[String]) : Options = options match {
    case Some(s) => {
      try {
        val json = Json.parse(s)
        (json\name) match {
          case QueryOptions(queryOpts) => queryOpts
          case _ => QueryOptions.DefaultOptions
        }
      } catch {
        case _ : Throwable => QueryOptions.DefaultOptions
      }
    }
    case _ => QueryOptions.DefaultOptions
  }

  private def getFieldValuesAsJsValue(name: String, q: Option[String], f: Option[String], c: String, sk: Int, l: Int): JsValue = {
    name match {
      case "subject" => {
        q.map(Subject.toSearchObj(_,None)).getOrElse[Either[SearchCancelled,MongoDBObject]](Right(MongoDBObject())) match {
          case Right(query) => f.map(Subject.toFieldsObj(_)) match {
            case Some(Right(searchFields)) => if(c == "true") JsObject(Seq("count" -> JsNumber(Subject.find(query).count)))
            else JsArray(Subject.find(query,searchFields.dbfields).toSeq.map(Json.toJson(_)))
            case None => if(c == "true") JsObject(Seq("count" -> JsNumber(Subject.find(query).count)))
            else JsArray(Subject.find(query).toSeq.map(Json.toJson(_)))
            case Some(Left(error)) => JsNull
          }
          case Left(sc) => sc.error match {
            case None => JsArray(Seq())
            case Some(error) => JsNull
          }
        }
      }
      case "cc-standard" => {
        q.map(Standard.toSearchObj(_,None)).getOrElse[Either[SearchCancelled,MongoDBObject]](Right(MongoDBObject())) match {
          case Right(query) => f.map(Standard.toFieldsObj(_)) match {
            case Some(Right(searchFields)) => if(c == "true") JsObject(Seq("count" -> JsNumber(Standard.find(query).count)))
            else JsArray(Standard.find(query,searchFields.dbfields).toSeq.map(Json.toJson(_)))
            case None => if(c == "true") JsObject(Seq("count" -> JsNumber(Standard.find(query).count)))
            else JsArray(Standard.find(query).toSeq.map(Json.toJson(_)))
            case Some(Left(error)) => JsNull
          }
          case Left(sc) => sc.error match {
            case None => JsArray(Seq())
            case Some(error) => JsNull
          }
        }
      }
      case _ => {
        Cache.getAs[FieldValue](FieldValueCacheKey) match {
          case None => {
            loadFieldValue()
            Cache.getAs[FieldValue](FieldValueCacheKey) match {
              case None => throw new RuntimeException("Unable to retrieve field value data")
              case Some(fv) => getSubFieldAsJsValue(Some(fv), name)
            }
          }
          case Some(fv) => getSubFieldAsJsValue(Some(fv), name)
        }
      }
    }

  }

  private def loadFieldValue() {
    FieldValue.findOne(MongoDBObject()) match {
      case Some(fv) => Cache.set(FieldValueCacheKey, fv)
      case _ => //do nothing
    }
  }

  private def getSubFieldAsJsValue(fieldValue: Option[FieldValue], fieldName: String): JsValue = fieldValue match {
    case Some(fv) => {
      FieldValue.getSeqForFieldName(fv, fieldName) match {
        case Some(json) => json
        case _ => JsObject(Seq())
      }
    }
    case _ => JsObject(Seq())
  }
}
