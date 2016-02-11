package org.corespring.v2.player.cdn

import org.joda.time.DateTime

class SignedUrlCdnResolver(
  domain: Option[String],
  version: Option[String],
  urlSigner: CdnUrlSigner,
  urlExpiresAfterMinutes: Int,
  httpProtocol: String = "") extends CdnResolver(domain, version) {

  override def resolveDomain(path: String): String = if (cdnDomain.isDefined) {
    val validUntil = DateTime.now().plusMinutes(urlExpiresAfterMinutes).toDate
    urlSigner.signUrl(httpProtocol + super.resolveDomain(path), validUntil)
  } else {
    path
  }

}