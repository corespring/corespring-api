package org.corespring.web.common.views.helpers

import com.typesafe.config.{ ConfigFactory, Config }

case class NewRelicConf(
  enabled: Boolean = false,
  licenseKey: String = "",
  applicationID: String = "",
  agent: String = "",
  scriptPath: String = "",
  sa: Int = 1,
  beacon: String = "bam.nr-data.net",
  errorBeacon: String = "bam.nr-data.net")

object NewRelicConf {
  lazy val config: NewRelicConf = getNewRelicConf("newrelic.rum.applications.cms")

  private def getNewRelicConf(prefix: String) = {

    val rootConfig: Config = ConfigFactory.load()

    def get(key: String): String = try {
      rootConfig.getString(prefix + "." + key)
    } catch {
      case _: Throwable => ""
    }

    NewRelicConf(
      enabled = get("enabled") == "true",
      licenseKey = get("license-key"),
      applicationID = get("application-id"),
      agent = get("agent"),
      scriptPath = get("script-path"))
  }
}
