package org.corespring.v2.auth

import org.bson.types.ObjectId
import play.api.mvc.RequestHeader
import scalaz.{ Failure, Success, Validation }

object ClientIdAndOptionsTransformer {
  object Keys {
    val apiClient = "apiClient"
    val options = "options"
    val skipDecryption = "skipDecryption"
  }
}
trait ClientIdAndOptionsTransformer[B] extends WithServiceOrgTransformer[B] {

  import ClientIdAndOptionsTransformer.Keys

  def clientIdToOrgId(apiClientId: String): Option[ObjectId]

  override def headerToOrgId(rh: RequestHeader): Validation[String, ObjectId] = {
    logger.trace("Try from query params")
    val out = for {
      apiClientId <- rh.getQueryString(Keys.apiClient)
      orgId <- clientIdToOrgId(apiClientId)
    } yield orgId

    out.map(Success(_)).getOrElse(Failure("No org loaded for apiClient + options"))
  }

}
