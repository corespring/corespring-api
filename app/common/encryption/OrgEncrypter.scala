package common.encryption

import org.bson.types.ObjectId
import org.corespring.platform.core.models.auth.ApiClient
import org.corespring.common.encryption.Crypto

class OrgEncrypter(orgId: ObjectId, encrypter: Crypto) {

  def encrypt(s: String): Option[EncryptionResult] = ApiClient.findOneByOrgId(orgId).map {
    client =>
      try{
        val data = encrypter.encrypt(s, client.clientSecret)
        EncryptionSuccess(client.clientId.toString, data, Some(s))
      }
      catch {
        case e : Throwable => EncryptionFailure("Error encrypting: ", e)
      }
  }


  def decrypt(s: String): Option[String] = ApiClient.findOneByOrgId(orgId).map {
    client =>
      encrypter.decrypt(s, client.clientSecret)
  }
}
