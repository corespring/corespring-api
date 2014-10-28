package org.corespring.web.common.views.helpers


case class NewRelicConf(
  licenseKey: String = "",
  applicationID: String = "",
  agent: String = "js-agent.newrelic.com/nr-476.min.js",
  sa: Int = 1,
  beacon: String = "bam.nr-data.net",
  errorBeacon: String = "bam.nr-data.net")
