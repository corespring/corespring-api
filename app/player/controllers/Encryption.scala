package player.controllers

import controllers.auth.BaseApi
import encryption.{MockUrlEncodeEncrypter, Encrypt}

class Encryption(encrypter: Encrypt) extends BaseApi {

  def encrypt = ApiAction {
    request =>
      request.body.asJson.map{
        json =>
          Ok(encrypter.encrypt(json.toString(), "some_key_here"))
      }.getOrElse(BadRequest("no json provided"))
  }
}

object Encryption extends Encryption(MockUrlEncodeEncrypter)



