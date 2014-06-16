package org.corespring.v2.auth

import org.bson.types.ObjectId
import play.api.mvc.RequestHeader
import scalaz.{Failure, Success, Validation}

trait ClientIdAndOptionsTransformer[B] extends WithOrgTransformer[B] {

  def clientIdToOrgId(apiClientId: String): Option[ObjectId]

  override def headerToOrgId(rh: RequestHeader): Validation[String, ObjectId] = {
    logger.trace("Try from query params")
    val out = for {
      apiClientId <- rh.getQueryString("apiClient")
      orgId <- clientIdToOrgId(apiClientId)
    } yield orgId

    out.map(Success(_)).getOrElse(Failure("No org loaded for apiClient + options"))
  }

}
