package org.corespring.encryption

import org.corespring.encryption.apiClient.{ MainApiClientEncryptionService, ApiClientEncryptionService }
import org.corespring.services.auth.ApiClientService

trait EncryptionModule {

  import com.softwaremill.macwire.MacwireMacros._

  def apiClientService: ApiClientService
  lazy val encrypt: EncryptDecrypt = AESEncryptDecrypt
  lazy val apiClientEncryptionService: ApiClientEncryptionService = wire[MainApiClientEncryptionService]
}
