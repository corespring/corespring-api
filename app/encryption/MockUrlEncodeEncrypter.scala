package encryption

object MockUrlEncodeEncrypter extends Encrypt with Decrypt {

  def encrypt(s:String,key:String) : String = java.net.URLEncoder.encode(s, "utf-8")

  def decrypt(s:String,key:String) : String = java.net.URLDecoder.decode(s, "utf-8")
}
