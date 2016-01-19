package org.corespring.encryption.apiClient

import grizzled.slf4j.Logger
import org.bson.types.ObjectId
import org.corespring.encryption.EncryptDecrypt
import org.corespring.models.auth.ApiClient
import org.corespring.services.auth.ApiClientService

trait ApiClientEncryptionService {
  def encrypt(apiClientId: String, s: String): Option[EncryptionResult]
  def encrypt(apiClient: ApiClient, s: String): Option[EncryptionResult]

  def decrypt(apiClientId: String, s: String): Option[String]
  def decrypt(apiClient: ApiClient, s: String): Option[String]
}

class MainApiClientEncryptionService(
  apiClientService: ApiClientService,
  encrypter: EncryptDecrypt) extends ApiClientEncryptionService {

  val logger = Logger(classOf[MainApiClientEncryptionService])

  override def encrypt(apiClientId: String, s: String): Option[EncryptionResult] =
    apiClientService.findByClientId(apiClientId).map(encrypt(_, s)).flatten

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
    apiClientService.findByClientId(apiClientId).map(decrypt(_, s)).flatten

  override def decrypt(apiCilent: ApiClient, s: String): Option[String] = {
    logger.debug(s"[ApiClientEncrypter] decrypt: $s with secret: ${apiCilent.clientSecret}")
    try {
      val out = encrypter.decrypt(s, apiCilent.clientSecret)
      logger.trace(s"[ApiClientEncrypter] result: $out")
      Some(out)
    } catch {
      case e: Exception => {
        val message: String = e.getMessage()
        logger.error(message)
        None
      }
    }
  }

}
