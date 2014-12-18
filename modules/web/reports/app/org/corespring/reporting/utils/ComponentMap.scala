package org.corespring.reporting.utils

import org.corespring.container.components.loader.{FileComponentLoader, ComponentLoader}
import play.api.Play._
import play.api.libs.json.{JsObject, JsValue}
import play.api.{Configuration, Mode, Play}

trait ComponentMap {

  private val componentLoader: ComponentLoader = {
    val containerConfig = {
      for {
        container <- current.configuration.getConfig("container")
        modeSpecific <- current.configuration.getConfig(s"container-${Play.current.mode.toString.toLowerCase}").orElse(Some(Configuration.empty))
      } yield {
        val out = container ++ modeSpecific ++ current.configuration.getConfig("v2.auth").getOrElse(Configuration.empty)
        out
      }
    }.getOrElse(Configuration.empty)

    val path = containerConfig.getString("components.path").toSeq

    val showReleasedOnlyComponents: Boolean = containerConfig.getBoolean("components.showReleasedOnly")
      .getOrElse {
      Play.current.mode == Mode.Prod
    }

    val out = new FileComponentLoader(path, showReleasedOnlyComponents)
    out.reload
    out
  }

  def componentMap = componentLoader.all.map(c => c.componentType -> c.packageInfo).filter{ case(k, v) => v match {
    case obj: JsObject => (obj \ "title").asOpt[String].nonEmpty
    case _ => false
  } }.map{ case(k, v) => k -> (v.asInstanceOf[JsObject] \ "title").as[String] }.toMap

}
