package org.corespring.platform.core.services.item

import org.corespring.container.components.loader.{FileComponentLoader, ComponentLoader}
import play.api.{Mode, Configuration, Play}
import play.api.libs.json._

class ComponentMap(application: play.api.Application) {

  def componentLoader: ComponentLoader = {
    val containerConfig = {
      for {
        container <- application.configuration.getConfig("container")
        modeSpecific <- application.configuration.getConfig(s"container-${application.mode.toString.toLowerCase}").orElse(Some(Configuration.empty))
      } yield {
        val out = container ++ modeSpecific ++ application.configuration.getConfig("v2.auth").getOrElse(Configuration.empty)
        out
      }
    }.getOrElse(Configuration.empty)

    val path = containerConfig.getString("components.path").toSeq

    val showReleasedOnlyComponents: Boolean = containerConfig.getBoolean("components.showReleasedOnly")
      .getOrElse { application.mode == Mode.Prod }

    val out = new FileComponentLoader(path, showReleasedOnlyComponents)
    out.reload
    out
  }

  def componentMap: Map[String, String] =
    componentLoader.all.map(c => c.componentType -> c.packageInfo).filter{ case(k, v) => v match {
      case obj: JsObject => (obj \ "title").asOpt[String].nonEmpty
      case _ => false
    } }.map{ case(k, v) => k -> (v.asInstanceOf[JsObject] \ "title").as[String] }.toMap

}

object ComponentMap extends ComponentMap(play.api.Play.current)