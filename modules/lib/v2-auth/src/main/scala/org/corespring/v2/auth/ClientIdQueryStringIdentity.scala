package org.corespring.v2.auth

import org.bson.types.ObjectId
import org.corespring.platform.core.models.Organization
import org.corespring.v2.auth.models.PlayerOptions
import play.api.libs.json.Json
import play.api.mvc.RequestHeader
import scalaz.{ Failure, Success, Validation }

object ClientIdQueryStringIdentity {

  object Keys {
    val apiClient = "apiClient"
    val options = "options"
    val skipDecryption = "skipDecryption"
  }

  object Errors {
    val noOrgForApiClient = "No org for api client"
  }

}

trait ClientIdQueryStringIdentity[B] extends OrgRequestIdentity[B] {

  import ClientIdQueryStringIdentity.Keys
  import ClientIdQueryStringIdentity.Errors

  def clientIdToOrgId(apiClientId: String): Option[ObjectId]

  def toPlayerOptions(orgId: ObjectId, rh: RequestHeader): PlayerOptions

  override def headerToOrgId(rh: RequestHeader): Validation[String, ObjectId] = {
    logger.trace("Try from query params")

    val maybeClientId = rh.getQueryString(Keys.apiClient)
    logger.trace( s"${Keys.apiClient} in query: ${maybeClientId.toString}")

    val out = for {
      apiClientId <- maybeClientId
      orgId <- clientIdToOrgId(apiClientId)
    } yield orgId

    logger.trace(s"result: $out")

    out.map(Success(_)).getOrElse(Failure(Errors.noOrgForApiClient))
  }
}

trait ClientIdAndOptsQueryStringWithDecrypt extends ClientIdQueryStringIdentity[(ObjectId, PlayerOptions)] {

  import ClientIdQueryStringIdentity.Keys

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

  override def toString = "[ClientIdAndOpts-QueryString-WithDecrypt]"
}

