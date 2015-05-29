package org.corespring.platform.core.encryption

import org.bson.types.ObjectId
import org.corespring.common.encryption.Crypto
import org.corespring.common.log.PackageLogging
import org.corespring.platform.core.models.auth.ApiClient
import play.api.Logger
import spray.caching.Cache

import scala.concurrent.{ Future, Await }

trait OrgEncryptionService {
  def encrypt(orgId: ObjectId, s: String): Option[EncryptionResult]
  def decrypt(orgId: ObjectId, s: String): Option[String]
}

class OrgEncrypter(apiClientEncryptionService: ApiClientEncryptionService) extends OrgEncryptionService {

  private val logger = Logger(classOf[OrgEncrypter])

  override def encrypt(orgId: ObjectId, s: String): Option[EncryptionResult] =
    ApiClient.findOneByOrgId(orgId).map(apiClientEncryptionService.encrypt(_, s)).flatten

  @deprecated("Please use apiClientId with ApiClientEncrypter, as this may not find the correct api client")
  override def decrypt(orgId: ObjectId, s: String): Option[String] =
    ApiClient.findOneByOrgId(orgId).map(apiClientEncryptionService.decrypt(_, s)).flatten

}

object OrgEncrypter {

  def apply(encrypter: Crypto) = new OrgEncrypter(new ApiClientEncrypter(encrypter))

}