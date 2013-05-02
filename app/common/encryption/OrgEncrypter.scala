package common.encryption

import models.auth.ApiClient
import org.bson.types.ObjectId

class OrgEncrypter(orgId: ObjectId, encrypter: Crypto) {

  def encrypt(s: String): Option[EncryptionResult] = ApiClient.findOneByOrgId(orgId).map {
    client =>
      val data = encrypter.encrypt(s, client.clientSecret)
      EncryptionResult(client.clientId.toString, data)
  }


  def decrypt(s: String): Option[String] = ApiClient.findOneByOrgId(orgId).map {
    client =>
      encrypter.decrypt(s, client.clientSecret)
  }
}
