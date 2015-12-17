package org.corespring.common.config

import play.api.Mode.Mode
import play.api.{ Configuration, Mode }

case class CdnConfig(
  addVersionAsQueryParam: Boolean,
  domain: Option[String],
  keyPairId: Option[String],
  privateKey: Option[String],
  signUrls: Boolean,
  urlValidInHours: Int)

object CdnConfig extends ConfigurationHelper {

  def apply(rootConfig: Configuration, mode: Mode): CdnConfig = {

    implicit val config = {
      rootConfig.getConfig("cdn")
    }.getOrElse(Configuration.empty)

    CdnConfig(
      getBoolean("add-version-as-query-param", Some(false)),
      getMaybeString("domain"),
      getMaybeString("key-pair-id"),
      getMaybeString("private-key"),
      getBoolean("sign-urls", Some(mode == Mode.Prod)),
      getInt("url-valid-in-hours", Some(24)))
  }
}
