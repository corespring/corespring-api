package developer.controllers

import play.api.Application
import securesocial.core.IdGenerator
import java.security.SecureRandom
import play.api.libs.Codecs

class CorespringIdGenerator(app: Application) extends IdGenerator(app) {

  val random = new SecureRandom()
  val IdSizeInBytes = 96

  def generate: String = {
    var randomValue = new Array[Byte](IdSizeInBytes)
    random.nextBytes(randomValue)
    Codecs.toHexString(randomValue)
  }
}
