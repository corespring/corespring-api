package player.controllers

import controllers.auth.BaseApi
import encryption.Encrypt

class Encryption(encrypter: Encrypt) extends BaseApi {

  def encrypt = ApiAction {
    request =>
      Ok(encrypter.encrypt("..", ".."))
  }
}

object NullEncrypter extends Encrypt {
  def encrypt(s: String, key: String): String = s
}

object Encryption extends Encryption(NullEncrypter)



