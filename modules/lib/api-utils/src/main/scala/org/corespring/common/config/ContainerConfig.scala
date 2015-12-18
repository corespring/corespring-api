package org.corespring.common.config

import play.api.{ Mode, Configuration }
import play.api.Mode.Mode

case class ContainerConfig(
  cdnAddVersionAsQueryParam: Boolean,
  cdnDomain: Option[String],
  componentsGzip: Boolean,
  componentsMinify: Boolean,
  componentsPath: String,
  debounceInMillis: String,
  showNonReleasedComponents: Boolean,
  config:Configuration )

object ContainerConfig extends ConfigurationHelper {

  def apply(rootConfig: Configuration, mode: Mode): ContainerConfig = {
    implicit val config = {
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

    ContainerConfig(
      getBoolean("cdn.add-version-as-query-param", false),
      getMaybeString("cdn.domain"),
      getBoolean("components.gzip", mode == Mode.Prod),
      getBoolean("components.minify", mode == Mode.Prod),
      getString("components.path"),
      getString("editor.autosave.debounceInMillis"),
      getBoolean("components.showNonReleasedComponents", mode == Mode.Dev),
      config)
  }
}
