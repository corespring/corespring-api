package org.corespring.encryption

trait EncryptDecrypt {
  def encrypt(message: String, privateKey: String): String
  def decrypt(encrypted: String, privateKey: String): String
}

object NullEncryptDecrypt extends EncryptDecrypt {
  def encrypt(s: String, key: String): String = s
  def decrypt(s: String, key: String): String = s
}
