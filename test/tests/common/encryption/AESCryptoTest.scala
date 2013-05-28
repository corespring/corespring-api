package tests.common.encryption

import common.encryption.AESCrypto
import org.specs2.mutable.Specification

class AESCryptoTest extends Specification {

  "AES Crypto" should {

    def works(key: String) = try {
      AESCrypto.decrypt(AESCrypto.encrypt("m", key), key) === "m"
    } catch {
      case e: Throwable => failure("error thrown for key: " + key)
    }

    "encrypt and decrypt" in {
      works("byzq4j0jpsjxmbnqk8w7wifv")
    }

  }

}
