package org.corespring.common.config

import play.api.Mode.Mode
import play.api.{ Configuration, Mode }

case class ItemAssetResolverConfig(
  addVersionAsQueryParam: Boolean,
  domain: Option[String],
  keyPairId: Option[String],
  privateKey: Option[String],
  signUrls: Boolean,
  urlValidInHours: Int)

object ItemAssetResolverConfig extends ConfigurationHelper {

  def apply(rootConfig: Configuration, mode: Mode): ItemAssetResolverConfig = {

    implicit val config = {
      rootConfig.getConfig("item-asset-resolver.cdn")
    }.getOrElse(Configuration.empty)

    ItemAssetResolverConfig(
      getBoolean("add-version-as-query-param", false),
      getMaybeString("domain"),
      getMaybeString("key-pair-id"),
      getMaybeString("private-key"),
      getBoolean("sign-urls", mode == Mode.Prod),
      getInt("url-valid-in-hours", Some(24)))
  }
}
