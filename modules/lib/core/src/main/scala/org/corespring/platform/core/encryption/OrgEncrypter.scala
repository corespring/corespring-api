package org.corespring.platform.core.encryption

import org.bson.types.ObjectId
import org.corespring.common.encryption.Crypto
import org.corespring.common.log.PackageLogging
import org.corespring.platform.core.models.auth.ApiClient

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

