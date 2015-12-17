package org.corespring.v2.player

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.security.PrivateKey
import java.util.Date

import com.amazonaws.auth.PEM
import com.amazonaws.services.cloudfront.CloudFrontUrlSigner
import org.joda.time.DateTime

class CdnUrlSigner(
  keyPairId: String,
  privateKeyString: String,
  urlValidInHours: Int ) {

  def signUrl(url: String): String = {
    val validUntil = DateTime.now().plusHours(urlValidInHours).toDate
    CloudFrontUrlSigner.getSignedURLWithCannedPolicy(
      url,
      keyPairId,
      privateKey,
      validUntil)
  }

  private lazy val privateKey: PrivateKey = {
    PEM.readPrivateKey(new ByteArrayInputStream(privateKeyString.getBytes(StandardCharsets.UTF_8)))
  }
}
