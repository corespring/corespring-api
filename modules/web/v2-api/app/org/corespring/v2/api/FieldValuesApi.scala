package org.corespring.v2.api

import org.corespring.itemSearch.ItemIndexService
import org.corespring.models.{ Standard, Subject }
import org.corespring.models.json.JsonFormatting
import org.corespring.services._
import org.corespring.v2.actions.V2Actions
import play.api.libs.json._
import play.api.mvc.{ Action }

import scala.concurrent.{ ExecutionContext, Future }
import scalaz.{ Failure, Success, Validation }

class FieldValuesApi(
  actions: V2Actions,
  indexService: ItemIndexService,
  v2ApiContext: V2ApiExecutionContext,
  standardService: StandardService,
  subjectService: SubjectService,
  jsonFormatting: JsonFormatting) extends V2Api {

  import jsonFormatting._

  override implicit def ec: ExecutionContext = v2ApiContext.context

  def contributors() = get(Keys.contributor)
  def gradeLevels() = get(Keys.gradeLevel)

  implicit val readSubjectQuery = Json.reads[SubjectQuery]

  implicit val readStandardQuery = Json.reads[StandardQuery]

  private object Keys {
    val contributor = "contributorDetails.contributor"
    val gradeLevel = "taskInfo.gradeLevel"
  }

  private def get(field: String) = actions.Org.async { request =>
    indexService.distinct(
      field,
      request.org.accessibleCollections.map(_.collectionId.toString)).map(_ match {
        case Success(contributors) => Ok(JsArray(contributors.map(JsString)))
        case Failure(error) => InternalServerError(error.getMessage)
      })
  }

  private def queryAction[Q <: Query, S](service: QueryService[S, Q])(q: Option[String] = None, l: Int = 50, sk: Int = 0)(implicit r: Reads[Q], w: Writes[S]) = Action.async { _ =>
    Future {

      q.map { queryString =>

        val out = for {
          json <- Validation.fromTryCatch(Json.parse(queryString)).leftMap(t => t.getMessage)
          query <- Validation.fromEither(json.validate[Q].asEither)
            .leftMap(errors => s"Json can't be read as a query: $queryString")
        } yield {
          val list = service.query(query, l, sk)
          Ok(Json.toJson(list))
        }

        out match {
          case Success(r) => r
          case Failure(e) => BadRequest(e)
        }
      }.getOrElse {
        Ok(Json.toJson(service.list(l, sk)))
      }
    }
  }

  val subject = queryAction[SubjectQuery, Subject](subjectService)(_, _, _)
  val standard = queryAction[StandardQuery, Standard](standardService)(_, _, _)

  def domain = actions.Org.async { _ =>
    import jsonFormatting.writeStandardDomains
    standardService.domains.map { sd =>
      Ok(Json.toJson(sd))
    }
  }

}
