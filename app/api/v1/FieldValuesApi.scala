package api.v1

import controllers.auth.BaseApi
import play.api.libs.json.Json._
import play.api.mvc.Action
import play.api.Play.current
import models.KeyValue.KeyValueWrites
import play.api.libs.json
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import models._
import com.novus.salat._
import com.novus.salat.global._
import controllers.auth.{Permission, BaseApi}
import play.api.Logger
import api.{InvalidFieldException, ApiError, QueryHelper}
import com.mongodb.util.JSONParseException
import org.bson.types.ObjectId
import com.mongodb.casbah.Imports._
import com.novus.salat._
import com.novus.salat.global._
import play.api.templates.Xml
import play.api.mvc.Result
import play.api.libs.json.Json
import com.typesafe.config.ConfigFactory
import scala.Some
import play.api.cache.Cache

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

      fieldName match {
        case "subject" => {
          QueryHelper.list[Subject, ObjectId](q, f, c, sk, l, Subject.queryFields, Subject.dao, Subject.SubjectWrites, None)
        }
        case "cc-standard" => {
          QueryHelper.list[Standard, ObjectId](q, f, c, sk, l, Standard.queryFields, Standard.dao, Standard.StandardWrites, None)
        }
        case _ => {
          Cache.getAs[FieldValue](FieldValueCacheKey) match {
            case None => {
              loadFieldValue()
              Cache.getAs[FieldValue](FieldValueCacheKey) match {
                case None => throw new RuntimeException("Unable to retrieve field value data")
                case fv: Some[FieldValue] => getSubField(fv, fieldName)
              }
            }
            case fv: Some[FieldValue] => getSubField(fv, fieldName)
          }
        }
      }
  }

  private def loadFieldValue() {
    FieldValue.collection.findOne() match {
      case Some(fv) => Cache.set(FieldValueCacheKey, grater[FieldValue].asObject(fv))
      case _ => //do nothing
    }
  }

  private def getSubField(fieldValue: Option[FieldValue], fieldName: String): Result = fieldValue match {
    case Some(fv) => {
      fieldName match {
        case FieldValue.GradeLevel => Ok(toJson(fv.gradeLevels))
        case FieldValue.ReviewsPassed => Ok(toJson(fv.reviewsPassed))
        case FieldValue.KeySkills => Ok(toJson(fv.keySkills))
        case FieldValue.ItemTypes => Ok(toJson(fv.itemTypes))
        case FieldValue.LicenseTypes => Ok(toJson(fv.licenseTypes))
        case FieldValue.PriorUses => Ok(toJson(fv.priorUses))
        case FieldValue.Credentials => Ok(toJson(fv.credentials))
        case _ => NotFound
      }
    }
    case _ => NotFound
  }
}
