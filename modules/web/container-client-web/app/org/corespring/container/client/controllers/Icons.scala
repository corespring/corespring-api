package org.corespring.container.client.controllers

import org.corespring.container.client.HasContainerContext
import org.corespring.container.client.integration.ContainerExecutionContext
import org.corespring.container.components.model.ComponentInfo
import org.corespring.container.components.services.ComponentService
import play.api.Logger
import play.api.mvc.{ Action, Controller }

import scala.concurrent.Future

class Icons(
  val containerContext: ContainerExecutionContext,
  componentService: ComponentService) extends Controller with HasContainerContext {

  private lazy val logger = Logger(classOf[Icons])

  val Split = """(.*?)-(.*)""".r

  def icon(iconName: String) = Action.async {
    implicit request =>
      Future {

        def matchingComponentInfo(c: ComponentInfo) = {
          val Split(org, name) = iconName
          val matches = c.id.name == name && c.id.org == org
          logger.debug(s"matches: $matches")
          matches
        }

        val bytes: Option[Array[Byte]] =
          componentService.interactions.find(matchingComponentInfo).map(_.icon) match {
            case Some(icon) => icon
            case _ => componentService.widgets.find(matchingComponentInfo).map(_.icon).get
          }

        bytes.map {
          b =>
            Ok(b).as("image/png")
        }.getOrElse(NotFound(""))
      }
  }
}
