package org.corespring.v2player.integration.hooks

import org.corespring.container.client.hooks.{ DataQueryHooks => ContainerDataQueryHooks }
import org.corespring.platform.core.models.{ Standard, Subject }
import org.corespring.platform.core.models.item.FieldValue
import org.corespring.platform.core.services.QueryService
import org.slf4j.LoggerFactory
import play.api.libs.json.{ JsObject, JsArray, JsValue, Json }
import play.api.mvc.RequestHeader

import scala.concurrent.Future

trait DataQueryHooks extends ContainerDataQueryHooks {

  lazy val logger = LoggerFactory.getLogger("v2.integration.DataQuery")

  def subjectQueryService: QueryService[Subject]

  def standardQueryService: QueryService[Standard]

  def fieldValueJson: JsObject

  def standardsTreeJson: JsArray

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
      case "standardsTree" => Right(standardsTreeJson)
      case _ => {
        logger.trace(s"fields: ${fieldValueJson.fields.map(_._1)}")
        if (fieldValueJson.fields.map(_._1).contains(topic)) {
          Right((fieldValueJson \ topic).as[JsArray])
        } else {
          Left(404, s"Can't find $topic")
        }
      }
    }
  }

}
