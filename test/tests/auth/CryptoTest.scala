package tests.auth

import controllers.auth.MyCrypto
import org.specs2.mutable.Specification

/**
 * Created with IntelliJ IDEA.
 * User: josh
 * Date: 4/15/13
 * Time: 9:29 AM
 * To change this template use File | Settings | File Templates.
 */
class CryptoTest extends Specification{
  val privateKey = "blahblahblahblahblahblah";
  val message = "secret message is secret";
  var encrypted = ""

  "encryption/decryption" should{
    "encrypt a message that is not equal to original message" in {
      encrypted = MyCrypto.encryptAES(message,privateKey)
      encrypted must not equalTo(message)
    }
    "decrypt a message that is equal to original message"  in {
      val decrypted = MyCrypto.decryptAES(encrypted,privateKey);
      decrypted must beEqualTo(message);
    }
  }

}
