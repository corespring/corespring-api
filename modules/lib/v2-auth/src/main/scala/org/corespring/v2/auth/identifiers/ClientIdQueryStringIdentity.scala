package org.corespring.v2.auth.identifiers

import org.bson.types.ObjectId
import org.corespring.platform.core.models.Organization
import org.corespring.v2.auth.identifiers.ClientIdQueryStringIdentity.Keys
import org.corespring.v2.auth.models.PlayerOptions
import org.corespring.v2.errors.Errors.{ invalidQueryStringParameter, noClientIdAndOptionsInQueryString }
import org.corespring.v2.errors.V2Error
import org.corespring.v2.log.V2LoggerFactory
import play.api.libs.json.Json
import play.api.mvc.RequestHeader

import scalaz.{ Failure, Success, Validation }

object ClientIdQueryStringIdentity {

  object Keys {
    val apiClient = "apiClient"
    val options = "options"
    val skipDecryption = "skipDecryption"
  }

}

trait ClientIdQueryStringIdentity[B] extends OrgRequestIdentity[B] {

  def clientIdToOrgId(apiClientId: String): Option[ObjectId]

  def toPlayerOptions(orgId: ObjectId, rh: RequestHeader): PlayerOptions

  override lazy val logger = V2LoggerFactory.getLogger("auth", "ClientIdQueryString")

  override def headerToOrgId(rh: RequestHeader): Validation[V2Error, ObjectId] = {
    logger.trace("Try from query params")

    if (rh.getQueryString("apiClientId").isDefined) {
      Failure(invalidQueryStringParameter("apiClientId", Keys.apiClient))
    } else {

      val maybeClientId = rh.getQueryString(Keys.apiClient)
      logger.trace(s"${Keys.apiClient} in query: ${maybeClientId.toString}")

      val out = for {
        apiClientId <- maybeClientId
        orgId <- clientIdToOrgId(apiClientId)
      } yield {
        logger.trace(s"found orgId: $orgId for ${Keys.apiClient}: $apiClientId")
        orgId
      }

      out.map(Success(_)).getOrElse(Failure(noClientIdAndOptionsInQueryString(rh)))
    }

  }
}

trait ClientIdAndOptsQueryStringWithDecrypt extends ClientIdQueryStringIdentity[(ObjectId, PlayerOptions)] {

  def decrypt(encrypted: String, orgId: ObjectId, header: RequestHeader): Option[String]

  def toPlayerOptions(orgId: ObjectId, rh: RequestHeader): PlayerOptions = {
    for {
      optsString <- rh.queryString.get(Keys.options).map(_.head)
      decrypted <- decrypt(optsString, orgId, rh)
      json <- try {
        Some(Json.parse(decrypted))
      } catch {
        case _: Throwable => None
      }
      playerOptions <- json.asOpt[PlayerOptions]
    } yield playerOptions
  }.getOrElse(PlayerOptions.NOTHING)

  override def data(rh: RequestHeader, org: Organization, defaultCollection: ObjectId): (ObjectId, PlayerOptions) = (org.id -> toPlayerOptions(org.id, rh))

}

