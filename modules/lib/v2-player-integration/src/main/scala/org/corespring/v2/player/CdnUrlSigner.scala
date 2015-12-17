package org.corespring.v2.player

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.security.PrivateKey
import java.util.Date

import com.amazonaws.auth.PEM
import com.amazonaws.services.cloudfront.CloudFrontUrlSigner

class CdnUrlSigner(
  keyPairId: Option[String],
  privateKeyString: Option[String]) {

  def signUrl(url: String, validUntil: Date): String = {
    if(!keyPairId.isDefined){
      throw new IllegalArgumentException("keyPairId is not defined")
    }
    if(!privateKeyString.isDefined){
      throw new IllegalArgumentException("privateKeyString is not defined")
    }

    CloudFrontUrlSigner.getSignedURLWithCannedPolicy(
      url,
      keyPairId.get,
      privateKey,
      validUntil)
  }

  lazy val privateKey: PrivateKey = {
    val s = privateKeyString.get
    PEM.readPrivateKey(new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8)))
  }
}
