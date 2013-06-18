package tests.common.encryption

import common.encryption.AESCrypto
import org.specs2.mutable.Specification

class AESCryptoTest extends Specification {
  val key = "thekey"
  val message = "themessage";
  var encrypted:String = null;
  "AES Crypto" should {

    "result in cipher text and iv delimeted by '--' when encrypting" in {
      encrypted = AESCrypto.encrypt(message,key)
      encrypted.split("--").length must beEqualTo(2)
    }
    "result in same as clear text when decrypting" in {
      val decrypted = AESCrypto.decrypt(encrypted,key)
      decrypted must beEqualTo(message)
    }
  }

}
