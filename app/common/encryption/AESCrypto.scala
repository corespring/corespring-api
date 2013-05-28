package common.encryption

import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import play.api.libs.Codecs

object AESCrypto extends Crypto {
  def KEY_LENGTH = 16

  def KEY_RADIX = 36

  val KEY_LENGTH_REQUIREMENT = this.getClass.getSimpleName + "the encryption key must be a string with 25 characters"

  /** this is required because BigInt.toByteArray is converted to a signed array of bytes, which results in extra padding on the array
    */
  def stripKeyPadding(key: Array[Byte]): Array[Byte] = key.reverse.take(KEY_LENGTH)

  /**
   * Encrypt a String with the AES encryption standard. Private key must have a length of 16 bytes
   * @param value The String to encrypt
   * @param privateKey The key used to encrypt
   * @return An hexadecimal encrypted string
   */
  def encrypt(value: String, privateKey: String): String = withCipherAndSpec(value, privateKey, Cipher.ENCRYPT_MODE) {
    cipher =>
      Codecs.toHexString(cipher.doFinal(value.getBytes("utf-8")))
  }


  /**
   * Decrypt a String with the AES encryption standard. Private key must have a length of 16 bytes
   * @param value An hexadecimal encrypted string
   * @param privateKey The key used to encrypt
   * @return The decrypted String
   */
  def decrypt(value: String, privateKey: String): String = withCipherAndSpec(value, privateKey, Cipher.DECRYPT_MODE) {
    (cipher) =>
      def hexStringToByte(hexString: String): Array[Byte] = {
        import org.apache.commons.codec.binary.Hex
        Hex.decodeHex(hexString.toCharArray())
      }
      new String(cipher.doFinal(hexStringToByte(value)))
  }

  private def withCipherAndSpec(value: String, key: String, mode: Int)(block: Cipher => String): String = {
    require(key != null && key.length == 25, KEY_LENGTH_REQUIREMENT)
    val raw = stripKeyPadding(BigInt(key, KEY_RADIX).toByteArray)
    val spec = new SecretKeySpec(raw, "AES")
    val cipher = Cipher.getInstance("AES")
    cipher.init(mode, spec)
    block(cipher)
  }
}
