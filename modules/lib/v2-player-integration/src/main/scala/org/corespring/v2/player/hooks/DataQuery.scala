package org.corespring.v2.player.hooks

import org.corespring.container.client.hooks.{ DataQueryHooks => ContainerDataQueryHooks }
import org.corespring.container.client.integration.ContainerExecutionContext
import org.corespring.models.json.JsonFormatting
import org.corespring.models.{ Standard, Subject }
import org.corespring.services.QueryService
import play.api.Logger
import play.api.libs.json.{ JsObject, JsArray, JsValue, Json }
import play.api.mvc.RequestHeader

import scala.concurrent.Future

case class StandardsTree(json: JsArray)

class DataQueryHooks(
  subjectQueryService: QueryService[Subject],
  standardQueryService: QueryService[Standard],
  standardsTree: StandardsTree,
  jsonFormatting: JsonFormatting,
  override implicit val ec: ContainerExecutionContext) extends ContainerDataQueryHooks {

  lazy val logger = Logger(classOf[DataQueryHooks])

  implicit val writeSubject = jsonFormatting.writeSubject
  implicit val formatSubjects = jsonFormatting.formatSubjects
  implicit val formatStandard = jsonFormatting.formatStandard

  override def findOne(topic: String, id: String)(implicit header: RequestHeader): Future[Either[(Int, String), Option[JsValue]]] = Future {
    logger.trace(s"findOne $topic id: $id")
    Right(subjectQueryService.findOne(id).map(Json.toJson(_)))
  }

  override def list(topic: String, query: Option[String])(implicit header: RequestHeader): Future[Either[(Int, String), JsArray]] = Future {
    logger.trace(s"list: $topic - query: $query")

    def q[A](service: QueryService[A]) = query.map(q => service.query(q)).getOrElse(service.list)
    def subjectQuery = q(subjectQueryService)
    def standardQuery = q(standardQueryService)

    import play.api.libs.json.Json.toJson
    topic match {
      case "subjects.primary" => Right(toJson(subjectQuery).as[JsArray])
      case "subjects.related" => Right(toJson(subjectQuery).as[JsArray])
      case "standards" => Right(toJson(standardQuery).as[JsArray])
      case "standardsTree" => Right(standardsTree.json)
      case _ => {
        implicit val fv = jsonFormatting.writesFieldValue
        val fieldValueJson = Json.toJson(jsonFormatting.fieldValue).as[JsObject]
        logger.debug(s"function=list, topic=$topic, fieldValueJson.fields=${fieldValueJson.fields.map(_._1)}")
        logger.trace(s"function=list topic=$topic, fieldValues: ${Json.prettyPrint(fieldValueJson)}")
        if (fieldValueJson.fields.map(_._1).contains(topic)) {
          Right((fieldValueJson \ topic).as[JsArray])
        } else {
          Left(404, s"Can't find $topic")
        }
      }
    }
  }

}
