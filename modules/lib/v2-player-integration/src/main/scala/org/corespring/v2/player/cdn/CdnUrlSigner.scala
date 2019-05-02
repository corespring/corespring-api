package org.corespring.v2.player.cdn

import java.io.{ BufferedReader, ByteArrayInputStream, InputStream, InputStreamReader }
import java.nio.charset.StandardCharsets._
import java.nio.charset.Charset
import java.security.PrivateKey
import java.util.Date

import com.amazonaws.auth.PEM
import com.amazonaws.services.cloudfront.CloudFrontUrlSigner
import play.api.Logger
import org.corespring.macros.DescribeMacro.describe

import scala.collection.mutable.ArrayBuffer
import scala.util.{ Failure, Success, Try }

class CdnUrlSigner(
  keyPairId: String,
  privateKeyString: String) {

  private val logger = Logger(classOf[CdnUrlSigner])

  def signUrl(url: String, validUntil: Date): String = {

    CloudFrontUrlSigner.getSignedURLWithCannedPolicy(
      url,
      keyPairId,
      privateKey,
      validUntil)
  }

  private lazy val privateKey: PrivateKey = {

    val r = getPrivateKey(UTF_8)
      .transform(Try(_), _ => getPrivateKey(ISO_8859_1))

    r match {
      case Success(pk) => pk
      case Failure(t) => {
        throw t
      }
    }

  }

  private def getPrivateKey(encoding: Charset) = Try {
    logger.info(describe(encoding))

    val prepped = privateKeyString.replace("\\n", "\n")
    val is = asInputStream(prepped, encoding)
    PEM.readPrivateKey(is)
  }

  private def asInputStream(s: String, encoding: Charset) = {
    new ByteArrayInputStream(s.getBytes(encoding))
  }
}
