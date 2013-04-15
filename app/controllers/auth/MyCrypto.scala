package controllers.auth

import javax.crypto.spec.SecretKeySpec
import javax.crypto.Cipher
import play.api.libs.Codecs

object MyCrypto {




  /**
   * Encrypt a String with the AES encryption standard. Private key must have a length of 16 bytes
   * @param value The String to encrypt
   * @param privateKey The key used to encrypt
   * @return An hexadecimal encrypted string
   */
  def encryptAES(value: String, privateKey: String): String = {
    val raw = privateKey.getBytes("utf-8")
    val skeySpec = new SecretKeySpec(raw, "AES")
    val cipher = Cipher.getInstance("AES")
    cipher.init(Cipher.ENCRYPT_MODE, skeySpec)
    Codecs.toHexString(cipher.doFinal(value.getBytes("utf-8")))
  }

  /**
   * Decrypt a String with the AES encryption standard. Private key must have a length of 16 bytes
   * @param value An hexadecimal encrypted string
   * @param privateKey The key used to encrypt
   * @return The decrypted String
   */
  def decryptAES(value: String, privateKey: String): String = {
    val raw = privateKey.getBytes("utf-8")
    val skeySpec = new SecretKeySpec(raw, "AES")
    val cipher = Cipher.getInstance("AES")
    cipher.init(Cipher.DECRYPT_MODE, skeySpec)
    def hexStringToByte(hexString: String): Array[Byte] = {
      import org.apache.commons.codec.binary.Hex;
      Hex.decodeHex(hexString.toCharArray());
    }
    new String(cipher.doFinal(hexStringToByte(value)))
  }
}
