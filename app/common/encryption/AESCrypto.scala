package common.encryption

import javax.crypto.Cipher
import javax.crypto.spec.{IvParameterSpec, SecretKeySpec}
import play.api.libs.Codecs
import java.util.UUID
import java.nio.{ByteOrder, ByteBuffer}
import org.apache.commons.codec.binary.{Hex, Base64}
import java.security.MessageDigest

object AESCrypto extends Crypto {
  def KEY_LENGTH = 16

  def KEY_RADIX = 36

  val KEY_LENGTH_REQUIREMENT = this.getClass.getSimpleName + "the encryption key must be a string with 25 characters"

  private def generateIV:Array[Byte] = {
    val uuid = UUID.randomUUID();
    val bytes:Array[Byte] = new Array[Byte](16)
    val bb = ByteBuffer.wrap(bytes)
    bb.order(ByteOrder.BIG_ENDIAN)
    bb.putLong(uuid.getLeastSignificantBits)
    bb.putLong(uuid.getMostSignificantBits)
    bytes
  }
  /**
   * Encrypt a String with the AES encryption standard
   * @param value The String to encrypt
   * @param privateKey The key used to encrypt
   * @return An hexadecimal encrypted string
   */
  def encrypt(value: String, privateKey: String): String = {
    val raw = MessageDigest.getInstance("MD5").digest(privateKey.getBytes("UTF-8"))
    val iv = generateIV
    val spec = new SecretKeySpec(raw, "AES")
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher.init(Cipher.ENCRYPT_MODE, spec, new IvParameterSpec(iv))
    Codecs.toHexString(cipher.doFinal(value.getBytes("utf-8")))+"--"+Codecs.toHexString(iv)
  }


  /**
   * Decrypt a String with the AES encryption standard
   * @param value An hexadecimal encrypted string
   * @param privateKey The key used to encrypt
   * @return The decrypted String
   */
  def decrypt(value: String, privateKey: String): String = {
    val parts = value.split("--")
    require(parts.length == 2, "must contain cipher text and initialization vector (iv) separated by the delimeter '--'")
    val message = parts(0); val iv = parts(1);
    val messageBytes = Hex.decodeHex(message.toCharArray())
    val ivBytes = Hex.decodeHex(iv.toCharArray())
    val raw = MessageDigest.getInstance("MD5").digest(privateKey.getBytes("UTF-8"))
    val spec = new SecretKeySpec(raw, "AES")
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher.init(Cipher.DECRYPT_MODE, spec, new IvParameterSpec(ivBytes))
    new String(cipher.doFinal(messageBytes))
  }
}
