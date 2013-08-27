package org.corespring.common.encryption

import org.specs2.mutable.Specification

class AESCryptoTest extends Specification {

  val key = "thekey"
  val message = "themessage"

  import AESCrypto._

  "AES Crypto" should {

    "result in cipher text and iv delimeted by '--' when encrypting" in {
      encrypt(message, key).split("--").length === 2
    }

    "result in same as clear text when decrypting" in {
      decrypt(encrypt(message, key), key) === message
    }
  }

}
