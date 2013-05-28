package common.encryption

import javax.crypto.spec.SecretKeySpec
import javax.crypto.{Mac, Cipher}
import play.api.libs.Codecs

object AESCrypto extends Crypto{
  //the required key length in bytes
  override def KEY_LENGTH = 16;
  override def KEY_RADIX = 36

  /**
   * this is required because BigInt.toByteArray is converted to a signed array of bytes, which results in extra padding on the array
   * @param privateKey
   * @return
   */
  private def stripKeyPadding(privateKey:Array[Byte]):Array[Byte] = {
    val reversedKey = privateKey.reverse
    val newKey = new Array[Byte](KEY_LENGTH)
    var i = 0;
    while(i < KEY_LENGTH){
      newKey(i) = reversedKey(i)
      i = i + 1;
    }
    newKey
  }
  /**
   * Encrypt a String with the AES encryption standard. Private key must have a length of 16 bytes
   * @param value The String to encrypt
   * @param privateKey The key used to encrypt
   * @return An hexadecimal encrypted string
   */
  def encrypt(value: String, privateKey: String): String = {
    val raw = stripKeyPadding(BigInt(privateKey,KEY_RADIX).toByteArray)
    val skeySpec = new SecretKeySpec(raw, "AES")
    val cipher = Cipher.getInstance("AES")
    val bs = cipher.getBlockSize
    cipher.init(Cipher.ENCRYPT_MODE, skeySpec)
    Codecs.toHexString(cipher.doFinal(value.getBytes("utf-8")))
  }

  /**
   * Decrypt a String with the AES encryption standard. Private key must have a length of 16 bytes
   * @param value An hexadecimal encrypted string
   * @param privateKey The key used to encrypt
   * @return The decrypted String
   */
  def decrypt(value: String, privateKey: String): String = {
    val raw = stripKeyPadding(BigInt(privateKey,KEY_RADIX).toByteArray)
    val skeySpec = new SecretKeySpec(raw, "AES")
    val cipher = Cipher.getInstance("AES")
    cipher.init(Cipher.DECRYPT_MODE, skeySpec)
    def hexStringToByte(hexString: String): Array[Byte] = {
      import org.apache.commons.codec.binary.Hex

      Hex.decodeHex(hexString.toCharArray());
    }
    new String(cipher.doFinal(hexStringToByte(value)))
  }
}
