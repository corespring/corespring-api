package tests.common.encryption

import org.specs2.mutable.Specification
import common.encryption.AESCrypto

class AESCryptoTest extends Specification {

  "AES Crypto" should {

    "encrypt and decrypt" in {
      val key = "byzq4j0jpsjxmbnqk8w7wifv"
      AESCrypto.decrypt(AESCrypto.encrypt("m", key), key) === "m"
    }
  }

}
