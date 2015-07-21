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
  def decrypt(apiCilent: ApiClient, s: String): Option[String]

  def encryptByOrg(orgId: ObjectId, s: String): Option[EncryptionResult]
}

trait MainApiClientEncryptionService extends ApiClientEncryptionService {

  def apiClientService: ApiClientService

  def encrypter: EncryptDecrypt

  val logger = Logger(classOf[MainApiClientEncryptionService])

  override def encrypt(apiClientId: String, s: String): Option[EncryptionResult] =
    apiClientService.findByKey(apiClientId).map(encrypt(_, s)).flatten

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
    apiClientService.findByKey(apiClientId).map(decrypt(_, s)).flatten

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

  /**
   * We should stop using this method. It is dangerous because there is no guarantee of what API client will be used.
   */
  override def encryptByOrg(orgId: ObjectId, s: String) = randomApiClientForOrg(orgId).map(encrypt(_, s)).flatten
  protected def randomApiClientForOrg(orgId: ObjectId) = apiClientService.findOneByOrgId(orgId)

}
