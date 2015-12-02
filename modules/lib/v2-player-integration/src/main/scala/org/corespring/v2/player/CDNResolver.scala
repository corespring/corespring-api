package org.corespring.v2.player

import play.api.Logger

class CDNResolver(domain: Option[String], version: Option[String]) {
  lazy val logger = Logger(classOf[CDNResolver])

  lazy val cdnDomain = domain.flatMap { d =>
    if (!d.startsWith("//")) {
      logger.warn("cdn domain must start with // - ignoring")
      None
    } else {
      logger.info(s"CDN for v2 production player: $d")
      Some(d)
    }
  }

  def resolveDomain(path: String): String = cdnDomain.map {
    d =>
      val trimmedPath = if (path.startsWith("/")) path.substring(1) else path
      val querySeparator = if (path.indexOf('?') >= 0) "&" else "?"
      val query = version.map { v => s"${querySeparator}version=$v" }.getOrElse("")
      s"$d/$trimmedPath$query"
  }.getOrElse(path)

}