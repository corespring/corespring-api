package org.corespring.common.config

import play.api.{ Mode, Configuration }
import play.api.Mode.Mode

case class ContainerConfig(rootConfig: Configuration, mode: Mode) extends ConfigurationHelper {

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

  lazy val cdnAddVersionAsQueryParam: Boolean = getBoolean("cdn.add-version-as-query-param", Some(false))
  lazy val cdnDomain: Option[String] = getMaybeString("cdn.domain")

  lazy val componentsGzip = getBoolean("components.gzip", Some(mode == Mode.Prod))
  lazy val componentsMinify = getBoolean("components.minify", Some(mode == Mode.Prod))
  lazy val componentsPath = getString("components.path")
  lazy val debounceInMillis = getString("editor.autosave.debounceInMillis")
  lazy val devToolsEnabled = getString("common.DEV_TOOLS_ENABLED")
  lazy val showNonReleasedComponents = getBoolean("components.showNonReleasedComponents", Some(mode == Mode.Dev))
}

