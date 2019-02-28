package org.corespring.container.client.controllers

import org.corespring.container.client.HasContainerContext

import scala.concurrent.{ ExecutionContext, Future }
import org.corespring.container.client.hooks.DataQueryHooks
import org.corespring.container.client.integration.ContainerExecutionContext
import play.api.libs.json.Json
import play.api.mvc.{ Action, AnyContent, Controller }

object DataQuery {

  val findTopics = Seq(
    "standards",
    "subjects.primary",
    "subjects.related")

  val listTopics = Seq(
    "bloomsTaxonomy",
    "credentials",
    "depthOfKnowledge",
    "gradeLevels",
    "itemTypes",
    "keySkills",
    "licenseTypes",
    "mediaType",
    "priorUses",
    "reviewsPassed",
    "standardClusters",
    "standards",
    "standardsTree",
    "subjects.primary",
    "subjects.related")
}

/** Query service for static data, eg: subject, gradelevel, etc */
class DataQuery(
  hooks: DataQueryHooks,
  val containerContext: ContainerExecutionContext) extends Controller with HasContainerContext {

  import org.corespring.container.client.controllers.DataQuery._

  /** list all that match the query - if there's no query list all */
  def list(topic: String, query: Option[String] = None): Action[AnyContent] = Action.async { implicit request =>

    if (listTopics.contains(topic)) {
      hooks.list(topic, query).map {
        case Left((code, msg)) => Status(code)(Json.obj("error" -> msg))
        case Right(arr) => Ok(arr)
      }
    } else {
      Future(BadRequest(Json.obj("error" -> s"$topic is not a valid topic from: ${listTopics.mkString(",")}")))
    }
  }

  def findOne(topic: String, id: String): Action[AnyContent] = Action.async {
    implicit request =>
      if (findTopics.contains(topic)) {
        hooks.findOne(topic, id).map {
          case Left((code, msg)) => Status(code)(Json.obj("error" -> msg))
          case Right(maybeData) => Ok(maybeData.getOrElse(Json.obj()))
        }
      } else {
        Future(BadRequest(Json.obj("error" -> s"$topic is not a valid topic from: ${findTopics.mkString(",")}")))
      }
      Future(Ok(""))
  }

}
