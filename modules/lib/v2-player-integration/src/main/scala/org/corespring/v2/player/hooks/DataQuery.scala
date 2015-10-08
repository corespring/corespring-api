package org.corespring.v2.player.hooks

import org.corespring.container.client.hooks.{ DataQueryHooks => ContainerDataQueryHooks }
import org.corespring.container.client.integration.ContainerExecutionContext
import org.corespring.models.json.JsonFormatting
import org.corespring.models.{ Standard, Subject }
import org.corespring.services.{ StandardService, StandardQuery, SubjectQuery, QueryService }
import play.api.Logger
import play.api.libs.json._
import play.api.mvc.{ RequestHeader }
import play.api.http.Status._

import scala.concurrent.Future
import scalaz.{ Success, Validation }
import scalaz.Scalaz._

case class StandardsTree(json: JsArray)

class DataQueryHooks(
  subjectQueryService: QueryService[Subject, SubjectQuery],
  standardQueryService: StandardService,
  standardsTree: StandardsTree,
  jsonFormatting: JsonFormatting,
  override implicit val containerContext: ContainerExecutionContext) extends ContainerDataQueryHooks {

  lazy val logger = Logger(classOf[DataQueryHooks])

  implicit val writeSubject = jsonFormatting.writeSubject
  implicit val formatSubjects = jsonFormatting.formatSubjects
  implicit val formatStandard = jsonFormatting.formatStandard

  override def findOne(topic: String, id: String)(implicit header: RequestHeader): Future[Either[(Int, String), Option[JsValue]]] = Future {
    logger.trace(s"findOne $topic id: $id")
    Right(subjectQueryService.findOne(id).map(Json.toJson(_)))
  }

  private def toSubjectQuery(json: JsValue): Validation[String, SubjectQuery] = for {
    jso <- json.asOpt[JsObject].toSuccess("json isn't a js object")
    term <- (json \ "searchTerm").asOpt[String].toSuccess("no 'searchTerm' in json")
  } yield {
    SubjectQuery(term,
      (json \ "filters" \ "subject").asOpt[String],
      (json \ "filters" \ "category").asOpt[String])
  }

  private def toStandardQuery(json: JsValue): Validation[String, StandardQuery] = for {
    jso <- json.asOpt[JsObject].toSuccess("json isn't a js object")
    term <- (json \ "searchTerm").asOpt[String].toSuccess("no 'searchTerm' in json")
  } yield {
    StandardQuery(
      term,
      (json \ "filters" \ "standard").asOpt[String],
      (json \ "filters" \ "subject").asOpt[String],
      (json \ "filters" \ "category").asOpt[String],
      (json \ "filters" \ "subCategory").asOpt[String])
  }

  override def list(topic: String, query: Option[String])(implicit header: RequestHeader): Future[Either[(Int, String), JsArray]] = Future {
    logger.trace(s"list: $topic - query: $query")

    lazy val subjectQueryResult: Validation[(Int, String), Stream[Subject]] = {
      query.map { queryString =>
        val result = for {
          json <- Validation.fromTryCatch(Json.parse(queryString)).leftMap(t => t.getMessage)
          query <- toSubjectQuery(json)
        } yield {
          subjectQueryService.query(query, 0, 0)
        }
        result.leftMap { e => (BAD_REQUEST -> e) }
      }.getOrElse {
        Success(subjectQueryService.list(0, 0))
      }
    }

    lazy val standardQueryResult: Validation[(Int, String), Stream[Standard]] = {
      query.map { queryString =>

        val json = Validation.fromTryCatch(Json.parse(queryString)).leftMap(t => t.getMessage)

        json.leftMap(e => BAD_REQUEST -> e).flatMap {
          jso =>
            {
              (jso \ "dotNotation").asOpt[String].map { dn =>
                Success(standardQueryService.queryDotNotation(dn, 0, 0))
              }.getOrElse {
                toStandardQuery(jso).bimap(
                  e => BAD_REQUEST -> e,
                  q => standardQueryService.query(q, 0, 0))
              }
            }
        }
      }.getOrElse {
        Success(standardQueryService.list(0, 0))
      }
    }

    def toResult[A](v: Validation[(Int, String), Stream[A]])(implicit w: Writes[A]): Either[(Int, String), JsArray] = {
      v.toEither.map(list => Json.arr(list.toSeq.map(Json.toJson(_))))
    }

    topic match {
      case "subjects.primary" => toResult(subjectQueryResult)
      case "subjects.related" => toResult(subjectQueryResult)
      case "standards" => toResult(standardQueryResult)
      case "standardsTree" => Right(standardsTree.json)
      case _ => {
        implicit val fv = jsonFormatting.writesFieldValue
        val fieldValueJson = Json.toJson(jsonFormatting.fieldValue).as[JsObject]
        logger.debug(s"function=list, topic=$topic, fieldValueJson.fields=${fieldValueJson.fields.map(_._1)}")
        logger.trace(s"function=list topic=$topic, fieldValues: ${Json.prettyPrint(fieldValueJson)}")
        if (fieldValueJson.fields.map(_._1).contains(topic)) {
          val out = (fieldValueJson \ topic)
          logger.trace(s"function=list, topic=$topic, out: $out")
          Right(out.as[JsArray])
        } else {
          Left(404, s"Can't find $topic")
        }
      }
    }
  }

}
