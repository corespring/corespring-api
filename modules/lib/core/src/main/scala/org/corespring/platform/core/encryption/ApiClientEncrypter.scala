package org.corespring.platform.core.encryption

import org.corespring.common.encryption.Crypto
import org.corespring.platform.core.models.auth.ApiClient
import play.api.Logger

trait ApiClientEncryptionService {
  def encrypt(apiClientId: String, s: String): Option[EncryptionResult]
  def encrypt(apiClient: ApiClient, s: String): Option[EncryptionResult]

  def decrypt(apiClientId: String, s: String): Option[String]
  def decrypt(apiCilent: ApiClient, s: String): Option[String]
}

class ApiClientEncrypter(encrypter: Crypto) extends ApiClientEncryptionService {

  private val logger = Logger(classOf[ApiClientEncrypter])

  override def encrypt(apiClientId: String, s: String): Option[EncryptionResult] =
    ApiClient.findByKey(apiClientId).map(encrypt(_, s)).flatten

  override def encrypt(apiClient: ApiClient, s: String): Option[EncryptionResult] = {
    val result = try {
      val data = encrypter.encrypt(s, apiClient.clientSecret)
      EncryptionSuccess(apiClient.clientId.toString, data, Some(s))
    } catch {
      case e: Throwable => EncryptionFailure("Error encrypting: ", e)
    }
    Some(result)
  }

  override def decrypt(apiClientId: String, s: String): Option[String] =
    ApiClient.findByKey(apiClientId).map(decrypt(_, s)).flatten

  override def decrypt(apiCilent: ApiClient, s: String): Option[String] = {
    logger.debug(s"[ApiClientEncrypter] decrypt: $s with secret: ${apiCilent.clientSecret}")
    val out = encrypter.decrypt(s, apiCilent.clientSecret)
    logger.trace(s"[ApiClientEncrypter] result: $out")
    Some(out)
  }

}