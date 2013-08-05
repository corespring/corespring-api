package common.encryption

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import play.api.libs.Codecs

object ShaHash {
  //the required key length in bytes
  def KEY_LENGTH = 16;
  def KEY_RADIX = 36


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
  def sign(message:String, privateKey:String):String = sign(message,privateKey.getBytes)
  /**
   * Signs the given String with HMAC-SHA1 using the given key.
   */
  def sign(message: String, key: Array[Byte]): String = {
    val mac = Mac.getInstance("HmacSHA1")
    mac.init(new SecretKeySpec(key, "HmacSHA1"))
    Codecs.toHexString(mac.doFinal(message.getBytes("utf-8")))
  }
}
