package org.corespring.common.config

import play.api.Mode.Mode
import play.api.{ Configuration, Mode }

case class ItemFileFilterConfig(
  enabled: Boolean,
  addVersionAsQueryParam: Boolean,
  domain: Option[String],
  signUrls: Boolean,
  keyPairId: Option[String],
  privateKey: Option[String],
  urlExpiresAfterMinutes: Int,
  httpProtocolForSignedUrls: String)

object ItemFileFilterConfig extends ConfigurationHelper {

  def apply(rootConfig: Configuration, mode: Mode): ItemFileFilterConfig = {

    implicit val config = {
      rootConfig.getConfig("item-file-filter")
    }.getOrElse(Configuration.empty)

    ItemFileFilterConfig(
      getBoolean("enabled", false),
      getBoolean("add-version-as-query-param", true),
      getMaybeString("domain"),
      getBoolean("sign-urls", false),
      getMaybeString("key-pair-id"),
      getMaybeString("private-key"),
      getInt("url-expires-after-minutes", 5),
      getString("http-protocol-for-signed-urls", "https:"))
  }
}
