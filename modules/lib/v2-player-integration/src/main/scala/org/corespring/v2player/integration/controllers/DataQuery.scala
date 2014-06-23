package org.corespring.v2player.integration.controllers

import org.corespring.container.client.controllers.{ DataQuery => ContainerDataQuery }
import org.corespring.platform.core.models.Subject
import org.corespring.platform.core.models.item.FieldValue
import org.corespring.platform.core.services.QueryService
import org.slf4j.LoggerFactory
import play.api.libs.json.{ JsArray, Json }
import play.api.mvc.{ Action, AnyContent }

trait DataQuery extends ContainerDataQuery {

  lazy val logger = LoggerFactory.getLogger("v2.integration.DataQuery")

  def subjectQueryService: QueryService[Subject]

  def fieldValues: FieldValue

  override def findOne(topic: String, id: String): Action[AnyContent] = Action {
    request =>

      logger.trace(s"findOne $topic id: $id")

      subjectQueryService.findOne(id).map {
        s =>
          Ok(Json.toJson(s))
      }.getOrElse(NotFound(""))
  }

  override def list(topic: String, query: Option[String]): Action[AnyContent] = Action {
    request =>

      logger.trace(s"list: $topic - query: $query")

      def subjectQuery = {
        query.map {
          q =>
            subjectQueryService.query(q)
        }.getOrElse(subjectQueryService.list)
      }

      val json = topic match {
        case "gradeLevel" => {
          Json.toJson(fieldValues.gradeLevels)
        }
        case "itemType" => Json.toJson(fieldValues.itemTypes)
        case "subjects.primary" => Json.toJson(subjectQuery)
        case "subjects.related" => Json.toJson(subjectQuery)
        case _ => Json.toJson(JsArray(Seq.empty))
      }
      Ok(json)
  }
}
