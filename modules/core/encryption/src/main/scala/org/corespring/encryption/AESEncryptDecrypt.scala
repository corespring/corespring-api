package org.corespring.encryption

import java.nio.{ ByteOrder, ByteBuffer }
import java.security.MessageDigest
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.spec.{ IvParameterSpec, SecretKeySpec }
import org.apache.commons.codec.binary.{ Hex }

object AESEncryptDecrypt extends EncryptDecrypt {
  def KEY_LENGTH = 16

  def KEY_RADIX = 36

  val KEY_LENGTH_REQUIREMENT = this.getClass.getSimpleName + "the encryption key must be a string with 25 characters"

  private def newIV: Array[Byte] = {
    val uuid = UUID.randomUUID()
    val bb = ByteBuffer.wrap(new Array[Byte](16)).order(ByteOrder.BIG_ENDIAN)
    bb.putLong(uuid.getLeastSignificantBits).putLong(uuid.getMostSignificantBits)
    bb.array()
  }

  private def getIV(encrypted: String): Array[Byte] = {
    val parts = encrypted.split("--")
    require(parts.length == 2, "must contain cipher text and initialization vector (iv) separated by the delimeter '--'")
    Hex.decodeHex(parts(1).toCharArray)
  }

  /**
   * Encrypt a String with the AES encryption standard
   * @param value The String to encrypt
   * @param privateKey The key used to encrypt
   * @return An hexadecimal encrypted string
   */
  def encrypt(value: String, privateKey: String): String = withCipher(privateKey, newIV, Cipher.ENCRYPT_MODE) {
    (cipher, iv) =>
      val bytes = cipher.doFinal(value.getBytes("utf-8"))
      val hexString = Hex.encodeHexString(bytes)
      s"$hexString--$iv"
  }

  /**
   * Decrypt a String with the AES encryption standard
   * @param value An hexadecimal encrypted string
   * @param privateKey The key used to encrypt
   * @return The decrypted String
   */
  def decrypt(value: String, privateKey: String): String = withCipher(privateKey, getIV(value), Cipher.DECRYPT_MODE) {
    (cipher, iv) =>
      new String(cipher.doFinal(Hex.decodeHex(value.split("--")(0).toCharArray)))
  }

  private def withCipher(key: String, iv: Array[Byte], mode: Int)(block: (Cipher, String) => String): String = {
    val raw = MessageDigest.getInstance("MD5").digest(key.getBytes("UTF-8"))
    val spec = new SecretKeySpec(raw, "AES")
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher.init(mode, spec, new IvParameterSpec(iv))
    block(cipher, new String(Hex.encodeHex(iv)))
  }
}
