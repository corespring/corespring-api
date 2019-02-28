package org.corespring.container.client

import play.api.Configuration
import play.api.libs.json.Json

case class NewRelicRumConfig(
  licenseKey: String,
  applicationID: String,
  agent: String,
  scriptPath: String,
  sa: Int = 1,
  beacon: String = "bam.nr-data.net",
  errorBeacon: String = "bam.nr-data.net") {
  val json = Json.writes[NewRelicRumConfig].writes(this)
}

object NewRelicRumConfig {
  def fromConfig(c: Configuration): Option[NewRelicRumConfig] = {
    for {
      enabled <- c.getBoolean("enabled")
      if enabled
      licenseKey <- c.getString("license-key")
      applicationID <- c.getString("application-id")
      agent <- c.getString("agent")
      scriptPath <- c.getString("script-path")
    } yield NewRelicRumConfig(licenseKey, applicationID, agent, scriptPath)
  }
}