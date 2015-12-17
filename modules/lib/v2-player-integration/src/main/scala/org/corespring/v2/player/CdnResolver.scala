package org.corespring.v2.player

import play.api.Logger

class CdnResolver(domain: Option[String], version: Option[String], urlSigner : Option[CdnUrlSigner] = None) {
  lazy val logger = Logger(classOf[CdnResolver])

  lazy val cdnDomain = domain.flatMap { d =>
    if (!d.startsWith("//")) {
      logger.warn("cdn domain must start with // - ignoring")
      None
    } else {
      logger.info(s"cdn for v2 production player: $d")
      Some(d)
    }
  }

  lazy val enabled = cdnDomain.isDefined

  def resolveDomain(path: String): String = cdnDomain.map {
    d =>
      val trimmedPath = if (path.startsWith("/")) path.substring(1) else path
      val querySeparator = if (path.indexOf('?') >= 0) "&" else "?"
      val query = version.map { v => s"${querySeparator}version=$v" }.getOrElse("")
      val url = s"$d/$trimmedPath$query"
      if(urlSigner.isDefined) urlSigner.get.signUrl(url) else url
  }.getOrElse(path)

}