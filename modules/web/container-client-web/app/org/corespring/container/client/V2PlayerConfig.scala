package org.corespring.container.client

import org.corespring.container.client.controllers.apps.CdnConfig

/**
 * @param rootUrl
 * @param newRelicRumConfig
 * @param launchTimeout - how long should the launcher wait for the underlying instance to be ready (default: 0 - aka no timeout)
 */
case class V2PlayerConfig(
  rootUrl: Option[String],
  newRelicRumConfig: Option[NewRelicRumConfig],
  launchTimeout: Int = 0,
  cdn: Option[CdnConfig]) {
  val useNewRelic = newRelicRumConfig.isDefined
}

