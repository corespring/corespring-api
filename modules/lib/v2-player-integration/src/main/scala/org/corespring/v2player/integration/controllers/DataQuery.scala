package org.corespring.v2player.integration.controllers

import org.corespring.container.client.controllers.{ DataQuery => ContainerDataQuery }
import org.corespring.platform.core.models.Subject
import org.corespring.platform.core.models.item.FieldValue
import org.corespring.platform.core.services.QueryService
import play.api.libs.json.{ JsArray, Json }
import play.api.mvc.{ AnyContent, Action }

trait DataQuery extends ContainerDataQuery {

  def subjectQueryService: QueryService[Subject]

  def fieldValues: FieldValue

  override def findOne(topic: String, id: String): Action[AnyContent] = Action {
    request =>
      subjectQueryService.findOne(id).map {
        s =>
          Ok(Json.toJson(s))
      }.getOrElse(NotFound(""))
  }

  override def list(topic: String, query: Option[String]): Action[AnyContent] = Action {
    request =>

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
        case "primarySubject" => Json.toJson(subjectQuery)
        case "relatedSubject" => Json.toJson(subjectQuery)
        case _ => Json.toJson(JsArray(Seq.empty))
      }
      Ok(json)
  }
}
