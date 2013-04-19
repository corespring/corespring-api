package encryption

trait Crypto {
  def KEY_LENGTH:Int = 16
  def KEY_RADIX:Int = 36

  def encrypt(message:String,privateKey:String) : String
  def decrypt(encrypted:String, privateKey:String) : String
}
