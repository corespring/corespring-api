package org.corespring.common.config

import play.api.Mode.Mode
import play.api.{ Configuration, Mode }

case class ItemFileFilterConfig(
  enabled: Boolean,
  addVersionAsQueryParam: Boolean,
  domain: Option[String])

object ItemFileFilterConfig extends ConfigurationHelper {

  def apply(rootConfig: Configuration, mode: Mode): ItemFileFilterConfig = {

    implicit val config = {
      rootConfig.getConfig("item-file-filter")
    }.getOrElse(Configuration.empty)

    ItemFileFilterConfig(
      getBoolean("enabled", false),
      getBoolean("add-version-as-query-param", false),
      getMaybeString("domain"))
  }
}
