package org.corespring.common.config

import play.api.Mode.Mode
import play.api.{Configuration, Mode}

case class ItemAssetResolverConfig(rootConfig: Configuration, mode: Mode) extends ConfigurationHelper {

  lazy val config = {
    rootConfig.getConfig("item-asset-resolver.cdn")
  }.getOrElse(Configuration.empty)

  lazy val cdnAddVersionAsQueryParam: Boolean = getBoolean("add-version-as-query-param", Some(false))
  lazy val cdnDomain: Option[String] = getMaybeString("domain")
  lazy val cdnKeyPairId: Option[String] = getMaybeString("key-pair-id")
  lazy val cdnPrivateKey: Option[String] = getMaybeString("private-key")
  lazy val cdnSignUrls: Boolean = getBoolean("sign-urls", Some(mode == Mode.Prod))
  lazy val cdnUrlValidInHours: Int = getInt("url-valid-in-hours", Some(24))
}

