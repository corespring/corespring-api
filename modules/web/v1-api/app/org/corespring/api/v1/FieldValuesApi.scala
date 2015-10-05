package org.corespring.api.v1

import org.corespring.models.json.JsonFormatting
import org.corespring.services._
import play.api.libs.json.{ JsObject, JsValue, Json }
import play.api.mvc.{ Action, Controller }

import scala.concurrent.{ ExecutionContext, Future }

import scalaz.Scalaz._
import scalaz.{ Failure, Success, Validation }

class FieldValuesApi(
  v2: org.corespring.v2.api.FieldValuesApi,
  standardService: StandardService,
  subjectService: SubjectService,
  jsonFormatting: JsonFormatting,
  ec: V1ApiExecutionContext) extends Controller {

  import jsonFormatting._

  implicit val context: ExecutionContext = ec.context

  def domain = v2.domain

  private def mapLegacyToQuery[Q <: Query](q: Option[String], mkQ: (String, JsObject) => Q): Future[Validation[String, Q]] = {
    Future {

      /**
       * Convert the old query format to a <SubjectQuery>
       * the old format has mongo-esque operators:
       * {"$or":[{"subject":{"$regex":"\\ba","$options":"i"}},{"category":{"$regex":"\\ba","$options":"i"}}]}
       * We only need the raw string - in the above example just 'a'.
       *
       * @param legacyFormat
       * @return
       */
      def legacyJsonToQ(legacyFormat: JsValue): Validation[String, Q] = {
        for {
          legacyJson <- legacyFormat.asOpt[JsObject].toSuccess("not a json object")
          list <- (legacyFormat \ "$or").asOpt[Seq[JsValue]].toSuccess("no $or in query")
          first <- list.headOption.toSuccess("$or list is empty")
          obj <- first.asOpt[JsObject].toSuccess("first entry isn't a json object")
          regex <- obj.fields.headOption.map(_._2).toSuccess("object is empty")
          regexString <- (regex \ "$regex").asOpt[String].toSuccess("no $regex field")
        } yield {
          val rawString = regexString.replace("\\\\b", "")
          mkQ(rawString, legacyJson)
        }
      }

      for {
        query <- q.toSuccess("no query passed in as 'q'")
        legacyFormat <- Validation.fromTryCatch(Json.parse(query)).leftMap(t => t.getMessage)
        newQuery <- legacyJsonToQ(legacyFormat)
      } yield newQuery
    }
  }

  def subject(q: Option[String], l: Int = 0, sk: Int = 0) = Action.async { request =>
    mapLegacyToQuery(q, (term, _) => SubjectQuery(term, None, None)).map { v =>
      v match {
        case Failure(e) => BadRequest(e)
        case Success(query) => {
          val list = subjectService.query(query, l, sk)
          Ok(Json.toJson(list))
        }
      }
    }
  }

  def standard(q: Option[String], l: Int = 0, sk: Int = 0) = {

    def mkStandardQuery(term: String, oldJson: JsObject): StandardQuery = {
      StandardQuery(term,
        standard = (oldJson \ "standard").asOpt[String],
        category = (oldJson \ "category").asOpt[String],
        subCategory = (oldJson \ "subCategory").asOpt[String],
        subject = (oldJson \ "subject").asOpt[String])
    }

    mapLegacyToQuery(q, mkStandardQuery).map { v =>
      v match {
        case Failure(e) => BadRequest(e)
        case Success(query) => {
          val list = standardService.query(query, l, sk)
          Ok(Json.toJson(list))
        }
      }
    }

  }
}
