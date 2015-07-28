package org.corespring.encryption

import org.corespring.encryption.apiClient.{ MainApiClientEncryptionService, ApiClientEncryptionService }
import org.corespring.services.auth.ApiClientService

trait EncryptionModule {

  import com.softwaremill.macwire.MacwireMacros._
  val apiClient: ApiClientService
  val encrypt: EncryptDecrypt = AESEncryptDecrypt
  val apiClientEncryption: ApiClientEncryptionService = wire[MainApiClientEncryptionService]
}
