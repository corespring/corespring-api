package api.v1

import play.api.mvc.Controller
import encryption.Encrypt
import controllers.auth.BaseApi

class EncryptionApi(encrypter:Encrypt) extends BaseApi{

  def encrypt = ApiAction{ request =>
    Ok("todo..")
  }
}

object NullEncrypter extends Encrypt{
  def encrypt(s:String,key:String) : String = s
}

object EncryptionApi extends EncryptionApi(NullEncrypter)


