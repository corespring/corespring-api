package common.encryption

trait Crypto {
  def KEY_LENGTH:Int = 16
  def KEY_RADIX:Int = 36

  def encrypt(message:String,privateKey:String) : String
  def decrypt(encrypted:String, privateKey:String) : String
}

object NullCrypto extends Crypto{
  def encrypt(s:String,key:String) : String = s
  def decrypt(s:String,key:String):String = s
}