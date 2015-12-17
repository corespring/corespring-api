package org.corespring.v2.player

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.security.PrivateKey
import java.util.Date

import com.amazonaws.auth.PEM
import com.amazonaws.services.cloudfront.CloudFrontUrlSigner

class CdnUrlSigner(
  keyPairId: String,
  privateKeyString: String) {

  def signUrl(url: String, validUntil: Date): String = {

    CloudFrontUrlSigner.getSignedURLWithCannedPolicy(
      url,
      keyPairId,
      privateKey,
      validUntil)
  }

  lazy val privateKey: PrivateKey = {
    PEM.readPrivateKey(new ByteArrayInputStream(privateKeyString.getBytes(StandardCharsets.UTF_8)))
  }
}
