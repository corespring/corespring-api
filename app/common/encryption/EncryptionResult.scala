package common.encryption

case class EncryptionResult(val clientId:String, val data:String, val requested:Option[String] = None)


