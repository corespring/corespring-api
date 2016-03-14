package org.corespring.v2.player.cdn

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.nio.charset.Charset
import java.security.PrivateKey
import java.util.Date

import com.amazonaws.auth.PEM
import com.amazonaws.services.cloudfront.CloudFrontUrlSigner

import scala.util.Try

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

  private lazy val privateKey: PrivateKey = {
    (getPrivateKey(StandardCharsets.UTF_8).orElse(getPrivateKey(StandardCharsets.ISO_8859_1)))
        .getOrElse(throw new RuntimeException(s"Unable to read private key from $privateKeyString"))
  }

  private def getPrivateKey(encoding: Charset) = {
    Try(PEM.readPrivateKey(asInputStream(privateKeyString, encoding)))
  }

  private def asInputStream(s: String, encoding:Charset) = {
     new ByteArrayInputStream(s.getBytes(encoding))
  }
}
