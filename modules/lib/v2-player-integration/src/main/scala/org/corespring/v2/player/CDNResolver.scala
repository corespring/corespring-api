package org.corespring.v2.player

import org.corespring.v2.log.V2LoggerFactory
import play.api.Configuration

class CDNResolver(val configuration: Configuration, version: String) {
  lazy val logger = V2LoggerFactory.getLogger("V2PlayerIntegration")

  lazy val cdnDomain = {
    val out = configuration.getString("cdn.domain")

    if (out.isDefined && !out.get.startsWith("//")) {
      logger.warn("cdn domain must start with // - ignoring")
    }
    val validDomain = out.filter(_.startsWith("//"))
    logger.info(s"CDN for v2 production player: ${validDomain.getOrElse("none")}")
    validDomain
  }

  def resolveDomain(path: String): String = cdnDomain.map {
    d =>
      val separator = if (path.startsWith("/")) "" else "/"
      val querySeparator = if (path.indexOf('?') >= 0) "&" else "?"
      val query = if (configuration.getBoolean("cdn.add-version-as-query-param").getOrElse(false)) s"${querySeparator}version=${version}" else ""
      s"$d$separator$path$query"
  }.getOrElse(path)

}