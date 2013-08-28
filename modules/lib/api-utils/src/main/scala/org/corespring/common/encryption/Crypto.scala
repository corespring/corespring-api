package org.corespring.common.encryption

trait Crypto {
  def encrypt(message: String, privateKey: String): String
  def decrypt(encrypted: String, privateKey: String): String
}

object NullCrypto extends Crypto {
  def encrypt(s: String, key: String): String = s
  def decrypt(s: String, key: String): String = s
}