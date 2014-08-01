package org.corespring.platform.core.encryption

import org.bson.types.ObjectId
import org.corespring.common.encryption.Crypto
import org.corespring.platform.core.models.auth.ApiClient
import org.corespring.common.log.PackageLogging

import scala.collection.mutable
import scalaz.Memo

class OrgEncrypter(orgId: ObjectId, encrypter: Crypto) extends PackageLogging {

  def encrypt(s: String): Option[EncryptionResult] = ApiClient.findOneByOrgId(orgId).map {
    client =>
      try {
        val data = encrypter.encrypt(s, client.clientSecret)
        EncryptionSuccess(client.clientId.toString, data, Some(s))
      } catch {
        case e: Throwable => EncryptionFailure("Error encrypting: ", e)
      }
  }

  def decrypt(s: String): Option[String] = ApiClient.findOneByOrgId(orgId).map {
    client =>
      logger.debug(s"decrypt: $s with secret: ${client.clientSecret}")
      val out = encrypter.decrypt(s, client.clientSecret)
      logger.debug(s"result: $out")
      out
  }
}

class MemoizedDecrypter(encrypter: Crypto) extends PackageLogging {

  import scalaz.Memo._

  val memo: Memo[(ObjectId, String), Option[String]] = weakHashMapMemo[(ObjectId, String), Option[String]]

  def decrypt(orgId: ObjectId, s: String): Option[String] = {

    logger.debug(s"decrypt: $orgId")
    memo {
      t =>
        val (orgId, s) = t

        logger.debug(s"find api client by org id: $orgId")
        ApiClient.findOneByOrgId(orgId).map {
          client =>
            val out = encrypter.decrypt(s, client.clientSecret)
            out
        }
    }.apply(orgId, s)

  }
}
