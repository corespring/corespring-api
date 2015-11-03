package org.corespring.common.config

import play.api.{Mode, Configuration}
import play.api.Mode.Mode

class ContainerConfig(rootConfig: Configuration, mode: Mode){

  lazy val config = {
    for {
      container <- rootConfig.getConfig("container")
      modeSpecific <- rootConfig
        .getConfig(s"container-${mode.toString.toLowerCase}")
        .orElse(Some(Configuration.empty))
      authConfig <- rootConfig
        .getConfig("v2.auth")
        .orElse(Some(Configuration.empty))
    } yield {
      val out = container ++ modeSpecific ++ authConfig
      out
    }
  }.getOrElse(Configuration.empty)

  private def getString(key: String, defaultValue:Option[String] = None): String = {
    config.getString(key).getOrElse(defaultValue.getOrElse(throw new RuntimeException(s"Key not found: $key")))
  }

  private def getBoolean(key: String, defaultValue:Option[Boolean] = None): Boolean = {
    config.getBoolean(key).getOrElse(defaultValue.getOrElse(throw new RuntimeException(s"Key not found: $key")))
  }

  lazy val cdnAddVersionAsQueryParam = getString("cdn.add-version-as-query-param")
  lazy val cdnDomain = getString("cdn.domain")
  lazy val componentsGzip = getBoolean("components.gzip", Some(mode == Mode.Prod))
  lazy val componentsMinify = getBoolean("components.minify", Some(mode == Mode.Prod))
  lazy val componentsPath = getString("components.path")
  lazy val debounceInMillis = getString("editor.autosave.debounceInMillis")
  lazy val devToolsEnabled = getString("common.DEV_TOOLS_ENABLED")
  lazy val showNonReleasedComponents = getBoolean("components.showNonReleasedComponents", Some(mode == Mode.Dev))
}


object ContainerConfig extends ContainerConfig(play.api.Play.current.configuration, play.api.Play.current.mode)