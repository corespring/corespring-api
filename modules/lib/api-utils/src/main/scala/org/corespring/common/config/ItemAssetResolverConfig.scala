package org.corespring.common.config

import play.api.Mode.Mode
import play.api.{Configuration, Mode}

case class ItemAssetResolverConfig(rootConfig: Configuration, mode: Mode) extends ConfigurationHelper {

  lazy val config = {
    rootConfig.getConfig("item-asset-resolver.cdn")
  }.getOrElse(Configuration.empty)

  lazy val addVersionAsQueryParam: Boolean = getBoolean("add-version-as-query-param", Some(false))
  lazy val domain: Option[String] = getMaybeString("domain")
  lazy val keyPairId: Option[String] = getMaybeString("key-pair-id")
  lazy val privateKey: Option[String] = getMaybeString("private-key")
  lazy val signUrls: Boolean = getBoolean("sign-urls", Some(mode == Mode.Prod))
  lazy val urlValidInHours: Int = getInt("url-valid-in-hours", Some(24))
}

