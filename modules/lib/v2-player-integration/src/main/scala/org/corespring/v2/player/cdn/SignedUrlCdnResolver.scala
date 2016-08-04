package org.corespring.v2.player.cdn

import org.joda.time.DateTime
import play.api.Logger
import org.corespring.macros.DescribeMacro.{ describe => ds }

class SignedUrlCdnResolver(
  domain: Option[String],
  version: Option[String],
  urlSigner: CdnUrlSigner,
  urlExpiresAfterMinutes: Int,
  httpProtocol: String = "") extends CdnResolver(domain, version) {

  private val logger = Logger(this.getClass)

  override def resolveDomain(path: String): String = if (cdnDomain.isDefined) {
    logger.trace(ds(path))
    val validUntil = DateTime.now().plusMinutes(urlExpiresAfterMinutes).toDate
    val signedUrl = urlSigner.signUrl(httpProtocol + super.resolveDomain(path), validUntil)
    logger.trace(ds(signedUrl))
    signedUrl
  } else {
    path
  }

}