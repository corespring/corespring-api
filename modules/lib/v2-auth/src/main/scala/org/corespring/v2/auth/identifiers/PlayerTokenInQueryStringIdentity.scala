package org.corespring.v2.auth.identifiers

import org.bson.types.ObjectId
import org.corespring.platform.core.models.Organization
import org.corespring.platform.core.models.auth.ApiClient
import org.corespring.v2.auth.identifiers.PlayerTokenInQueryStringIdentity.Keys
import org.corespring.v2.auth.models.{ AuthMode, OrgAndOpts, PlayerAccessSettings }
import org.corespring.v2.errors.V2Error
import org.corespring.v2.errors.Errors.{ generalError, missingRequiredField, invalidQueryStringParameter, noApiClientAndPlayerTokenInQueryString }
import org.corespring.v2.log.V2LoggerFactory
import org.corespring.v2.warnings.V2Warning
import org.corespring.v2.warnings.Warnings.deprecatedQueryStringParameter
import play.api.libs.json.{ JsSuccess, JsValue, JsError, Json }
import play.api.mvc.RequestHeader
import scalaz.{ Failure, Success, Validation }

object PlayerTokenInQueryStringIdentity {

  object Keys {
    val apiClient = "apiClient"
    /** deprecated("Still supported but is going to be removed", "1.1") */
    val options = "options"
    val playerToken = "playerToken"
    val skipDecryption = "skipDecryption"
  }
}

trait ApiClientPlayerTokenInQueryStringIdentity extends ApiClientRequestIdentity[ApiClient] {

  def clientIdToApiClient(apiClientId: String): Option[ApiClient]

  override def headerToApiClient(rh: RequestHeader): Validation[V2Error, ApiClient] = {
    logger.trace(s"function=headerToOrgId path=${rh.path}")

    if (rh.getQueryString("apiClientId").isDefined) {
      Failure(invalidQueryStringParameter("apiClientId", Keys.apiClient))
    } else {

      val maybeClientId = rh.getQueryString(Keys.apiClient)
      logger.trace(s"function=headerToOrgId ${Keys.apiClient}=${maybeClientId.toString}")

      val out = for {
        apiClientId <- maybeClientId
        apiClient <- clientIdToApiClient(apiClientId)
      } yield {
        logger.trace(s"function=headerToApiClient apiClient=${apiClientId} ${Keys.apiClient}=$apiClientId")
        apiClient
      }
      out.map(Success(_)).getOrElse(Failure(noApiClientAndPlayerTokenInQueryString(rh)))
    }
  }
}

trait PlayerTokenInQueryStringIdentity extends OrgRequestIdentity[OrgAndOpts] {

  override lazy val logger = V2LoggerFactory.getLogger("auth", "PlayerTokenInQueryStringIdentity")

  override def data(rh: RequestHeader, org: Organization) = {
    val (accessSettings, maybeWarning) = toAccessSettings(org.id, rh)
    OrgAndOpts(org, accessSettings, AuthMode.ClientIdAndPlayerToken, maybeWarning.toSeq)
  }

  /** for a given apiClient return the org Id */
  def clientIdToOrg(apiClientId: String): Option[Organization]

  /**
   * read the player token from the request header
   * Checks 'playerToken' param first and falls back to the deprecated 'options' param.
   * If using the deprecated param a V2Warning will be included.
   * @param rh
   * @return
   */
  def playerToken(rh: RequestHeader): Option[(String, Option[V2Warning])] = {

    val token = rh.getQueryString(Keys.playerToken)

    token.map(t => (t, None)).orElse {
      val deprecatedOptions = rh.getQueryString(Keys.options)
      deprecatedOptions.foreach { _ =>
        logger.warn(s"path=${rh.path} ${Keys.apiClient}=${rh.getQueryString(Keys.apiClient)} - Query string contains 'options' parameter. Inform client to use 'playerToken' instead.")
      }
      deprecatedOptions.map { t => (t, Some(deprecatedQueryStringParameter(Keys.options, Keys.playerToken))) }
    }
  }

  override def headerToOrg(rh: RequestHeader): Validation[V2Error, Organization] = {
    logger.trace(s"function=headerToOrgId path=${rh.path}")

    if (rh.getQueryString("apiClientId").isDefined) {
      Failure(invalidQueryStringParameter("apiClientId", Keys.apiClient))
    } else {

      val maybeClientId = rh.getQueryString(Keys.apiClient)
      logger.trace(s"function=headerToOrgId ${Keys.apiClient}=${maybeClientId.toString}")

      val out = for {
        apiClientId <- maybeClientId
        org <- clientIdToOrg(apiClientId)
      } yield {
        logger.trace(s"function=headerToOrg org=${org.id} orgName=${org.name} ${Keys.apiClient}=$apiClientId")
        org
      }
      out.map(Success(_)).getOrElse(Failure(noApiClientAndPlayerTokenInQueryString(rh)))
    }
  }

  def decrypt(encrypted: String, orgId: ObjectId, header: RequestHeader): Option[String]

  /** convert the orgId and header into PlayerOptions */
  protected def toAccessSettings(orgId: ObjectId, rh: RequestHeader): (PlayerAccessSettings, Option[V2Warning]) = {

    def toSettings(json: JsValue): Option[PlayerAccessSettings] = PlayerAccessSettings.format.reads(json) match {
      case JsError(errs) => {
        //TODO: Should we be returning a V2Error here?
        logger.warn(s"path=${rh.path} orgId=$orgId - Error parsing PlayerAccessSettings: ${errs.mkString(", ")}")
        Some(PlayerAccessSettings.NOTHING)
      }
      case JsSuccess(s, _) => Some(s)
    }

    for {
      tokenWithWarning <- playerToken(rh)
      decrypted <- decrypt(tokenWithWarning._1, orgId, rh)
      json <- try {
        Some(Json.parse(decrypted))
      } catch {
        case _: Throwable => {
          logger.error(s"Error parsing decrypted options: $decrypted")
          None
        }
      }
      playerOptions <- toSettings(json)
    } yield {
      logger.trace(s"decrypted: $decrypted")
      (playerOptions -> tokenWithWarning._2)
    }
  }.getOrElse {
    logger.trace(s"queryString -> ${rh.queryString}")
    logger.warn(s"restricting player option access for $orgId")
    (PlayerAccessSettings.NOTHING -> None)
  }

}

