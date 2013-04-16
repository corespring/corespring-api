package tests.auth

import controllers.auth.AESCrypto
import org.specs2.mutable.Specification
import java.nio.charset.Charset

/**
 * Created with IntelliJ IDEA.
 * User: josh
 * Date: 4/15/13
 * Time: 9:29 AM
 * To change this template use File | Settings | File Templates.
 */
class CryptoTest extends Specification{
  val privateKey = BigInt.probablePrime(AESCrypto.KEY_LENGTH*8, scala.util.Random).toString(AESCrypto.KEY_RADIX);
  val message = "secret message is secret";
  var encrypted = ""

  "encryption/decryption" should{
    "encrypt a message that is not equal to original message" in {
      encrypted = AESCrypto.encryptAES(message,privateKey)
      encrypted must not equalTo(message)
    }
    "decrypt a message that is equal to original message"  in {
      val decrypted = AESCrypto.decryptAES(encrypted,privateKey);
      decrypted must beEqualTo(message);
    }
  }

}
