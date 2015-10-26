package org.corespring.web.common.views.helpers

import com.typesafe.config.{ConfigFactory, Config}

case class NewRelicConf(
  enabled: Boolean = false,
  licenseKey: String = "",
  applicationID: String = "",
  agent: String = "js-agent.newrelic.com/nr-476.min.js",
  sa: Int = 1,
  beacon: String = "bam.nr-data.net",
  errorBeacon: String = "bam.nr-data.net")

object NewRelicConf{
  lazy val config: NewRelicConf = getNewRelicConf()

  private val rootConfig: Config = ConfigFactory.load()

  private def get(k: String): Option[String] = try {
    Some(rootConfig.getString(k))
  } catch {
    case _: Throwable => None
  }

  def getNewRelicConf() = {
    new NewRelicConf(
      enabled = get("newrelic.enabled").getOrElse("false") == "true",
      licenseKey = get("newrelic.license-key").getOrElse(""),
      applicationID = get("newrelic.application-id").getOrElse(""))

}
}
