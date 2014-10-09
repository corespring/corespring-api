package org.corespring.v2.auth.identifiers

import org.bson.types.ObjectId
import org.corespring.platform.core.models.Organization
import org.corespring.v2.auth.identifiers.PlayerTokenInQueryStringIdentity.Keys
import org.corespring.v2.auth.models.{ AuthMode, OrgAndOpts, PlayerAccessSettings }
import org.corespring.v2.errors.V2Error
import org.corespring.v2.errors.Errors.{ generalError, missingRequiredField, invalidQueryStringParameter, noapiClientAndPlayerTokenInQueryString }
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

trait PlayerTokenInQueryStringIdentity extends OrgRequestIdentity[OrgAndOpts] {

  override lazy val logger = V2LoggerFactory.getLogger("auth", "PlayerTokenInQueryStringIdentity")

  override def data(rh: RequestHeader, org: Organization, defaultCollection: ObjectId) = {
    val (accessSettings, maybeWarning) = toAccessSettings(org.id, rh)
    OrgAndOpts(org.id, accessSettings, AuthMode.ClientIdAndPlayerToken, Some(org), maybeWarning.toSeq)
  }

  /** for a given apiClient return the org Id */
  def clientIdToOrgId(apiClientId: String): Option[ObjectId]

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

  override def headerToOrgId(rh: RequestHeader): Validation[V2Error, ObjectId] = {
    logger.trace(s"function=headerToOrgId path=${rh.path}")

    if (rh.getQueryString("apiClientId").isDefined) {
      Failure(invalidQueryStringParameter("apiClientId", Keys.apiClient))
    } else {

      val maybeClientId = rh.getQueryString(Keys.apiClient)
      logger.trace(s"function=headerToOrgId ${Keys.apiClient}=${maybeClientId.toString}")

      val out = for {
        apiClientId <- maybeClientId
        orgId <- clientIdToOrgId(apiClientId)
      } yield {
        logger.trace(s"function=headerToOrgId orgId=$orgId ${Keys.apiClient}=$apiClientId")
        orgId
      }
      out.map(Success(_)).getOrElse(Failure(noapiClientAndPlayerTokenInQueryString(rh)))
    }
  }

  def decrypt(encrypted: String, orgId: ObjectId, header: RequestHeader): Option[String]

  /** convert the orgId and header into PlayerOptions */
  protected def toAccessSettings(orgId: ObjectId, rh: RequestHeader): (PlayerAccessSettings, Option[V2Warning]) = {

    def toSettings(json: JsValue): Option[PlayerAccessSettings] = PlayerAccessSettings.optionsFormat.reads(json) match {
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

