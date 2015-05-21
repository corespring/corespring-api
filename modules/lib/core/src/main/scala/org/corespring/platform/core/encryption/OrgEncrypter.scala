package org.corespring.platform.core.encryption

import org.bson.types.ObjectId
import org.corespring.common.encryption.Crypto
import org.corespring.common.log.PackageLogging
import org.corespring.platform.core.models.auth.ApiClient
import play.api.Logger
import play.api.libs.json.{JsObject, Json}
import spray.caching.Cache

import scala.concurrent.{ Future, Await }

trait OrgEncryptionService {
  def encrypt(orgId: ObjectId, s: String): Option[EncryptionResult]
  def decrypt(orgId: ObjectId, s: String): Option[String]
}

class OrgEncrypter(encrypter: Crypto) extends OrgEncryptionService {

  private val logger = Logger(classOf[OrgEncrypter])

  override def encrypt(orgId: ObjectId, s: String): Option[EncryptionResult] = ApiClient.findOneByOrgId(orgId).map {
    client =>
      try {
        logger.debug(s"function=encrypt code=$s")
        val data = encrypter.encrypt(s, client.clientSecret)
        EncryptionSuccess(client.clientId.toString, data, Some(s))
      } catch {
        case e: Throwable => EncryptionFailure("Error encrypting: ", e)
      }
  }

  override def decrypt(orgId: ObjectId, s: String): Option[String] = ApiClient.findOneByOrgId(orgId).map {
    client => {
      logger.debug(s"[OrgEncrypter] decrypt: $s with secret: ${client.clientSecret}")
      val out = encrypter.decrypt(s, client.clientSecret)
      logger.trace(s"[OrgEncrypter] result: $out")
      allowAnySession(out)
    }
  }

  /**
   * TODO - Remove as soon as possible.
   *
   * This method will allow any session id regardless of the options that are passed into it.
   */
  private def allowAnySession(string: String): String = try {
    (Json.parse(string) match {
      case options: JsObject => options.deepMerge(Json.obj(
        "sessionId" -> "*"
      )).toString
      case _ => string
    }).toString
  } catch {
    case e: Exception => string
  }

}

