package org.corespring.v2.auth.identifiers

import org.bson.types.ObjectId
import org.corespring.platform.core.models.{User, Organization}
import org.corespring.v2.auth.models.{AuthMode, PlayerAccessSettings, OrgAndOpts}
import org.corespring.v2.errors.Errors.{generalError, incorrectJsonFormat, noApiClientAndPlayerTokenInQueryString, invalidQueryStringParameter}
import org.corespring.v2.errors.V2Error
import org.corespring.v2.warnings.V2Warning
import org.corespring.v2.warnings.Warnings.deprecatedQueryStringParameter
import play.api.libs.json.{Json, JsSuccess, JsError, JsValue}
import play.api.mvc.RequestHeader

import scalaz.{Scalaz, Success, Failure, Validation}

object QueryStringIdentity {

  object Keys {
    val apiClient = "apiClient"
    /** deprecated("Still supported but is going to be removed", "1.1") */
    val options = "options"
    val playerToken = "playerToken"
    val skipDecryption = "skipDecryption"
  }

}

/**
 * An OrgRequestIdentity which handles loading identity from a query string. The
 * getQueryString(rh: RequestHeader): Map[String, Seq[String]] is unimplemented, as the query string may come from
 * different pieces of the Request, not only the url.
 */
trait QueryStringIdentity extends OrgRequestIdentity[OrgAndOpts] {

  import QueryStringIdentity.Keys

  def getQueryString(rh: RequestHeader): Map[String, Seq[String]]
  def getQueryString(rh: RequestHeader, key: String): Option[String] =
    getQueryString(rh).get(key).map(_.headOption).flatten

  override def data(rh: RequestHeader, org: Organization, apiClientId: Option[String], user: Option[User]): Validation[V2Error, OrgAndOpts] = {
    toAccessSettings(org.id, rh).map { tuple: (PlayerAccessSettings, Option[V2Warning]) =>
      val (accessSettings, maybeWarning) = tuple
      OrgAndOpts(
        org,
        accessSettings,
        AuthMode.ClientIdAndPlayerToken,
        apiClientId,
        None,
        maybeWarning.toSeq)
    }
  }

  /** get the apiClient if available */
  override def headerToApiClientId(rh: RequestHeader): Option[String] = getQueryString(rh, Keys.apiClient)

  /** for a given apiClient return the org Id */
  def clientIdToOrg(apiClientId: String): Option[Organization]

  /**
   * read the player token from the query string
   * Checks 'playerToken' param first and falls back to the deprecated 'options' param.
   * If using the deprecated param a V2Warning will be included.
   * @param rh
   * @return
   */
  def playerToken(rh: RequestHeader): Option[(String, Option[V2Warning])] = {

    val token = getQueryString(rh, Keys.playerToken)

    token.map(t => (t, None)).orElse {
      val deprecatedOptions = getQueryString(rh, Keys.options)
      deprecatedOptions.foreach { _ =>
        logger.warn(s"path=${rh.path} ${Keys.apiClient}=${getQueryString(rh, Keys.apiClient)} - Query string contains 'options' parameter. Inform client to use 'playerToken' instead.")
      }
      deprecatedOptions.map { t => (t, Some(deprecatedQueryStringParameter(Keys.options, Keys.playerToken))) }
    }
  }

  override def headerToOrgAndMaybeUser(rh: RequestHeader): Validation[V2Error, (Organization, Option[User])] = {
    logger.trace(s"function=headerToOrgId path=${rh.path}")

    if (getQueryString(rh).contains("apiClientId")) {
      Failure(invalidQueryStringParameter("apiClientId", Keys.apiClient))
    } else {

      val maybeClientId = getQueryString(rh, Keys.apiClient)
      logger.trace(s"function=headerToOrgId ${Keys.apiClient}=${maybeClientId.toString}")

      val out = for {
        apiClientId <- maybeClientId
        org <- clientIdToOrg(apiClientId)
      } yield {
        logger.trace(s"function=headerToOrg org=${org.id} orgName=${org.name} ${Keys.apiClient}=$apiClientId")
        org
      }
      out.map(Success(_, None))
        .getOrElse(Failure(noApiClientAndPlayerTokenInQueryString(rh)))
    }
  }

  def decrypt(encrypted: String, orgId: ObjectId, header: RequestHeader): Option[String]

  /** convert the orgId and header into PlayerOptions */
  protected def toAccessSettings(orgId: ObjectId, rh: RequestHeader): Validation[V2Error, (PlayerAccessSettings, Option[V2Warning])] = {

    def toSettings(json: JsValue): Validation[V2Error, PlayerAccessSettings] = PlayerAccessSettings.format.reads(json) match {
      case JsError(errs) => {
        logger.warn(s"path=${rh.path} orgId=$orgId - Error parsing PlayerAccessSettings: ${errs.mkString(", ")}")

        Failure(incorrectJsonFormat(json, Some(JsError(errs))))
      }
      case JsSuccess(s, _) => Success(s)
    }

    import Scalaz._

    val result: Validation[V2Error, (PlayerAccessSettings, Option[V2Warning])] = for {
      tokenWithWarning <- playerToken(rh).toSuccess(generalError("can't create player token"))
      decrypted <- decrypt(tokenWithWarning._1, orgId, rh).toSuccess(generalError("failed to decrypt"))
      json <- try {
        Success(Json.parse(decrypted))
      } catch {
        case _: Throwable => {
          logger.error(s"Error parsing decrypted options: $decrypted")
          Failure(generalError("Failed to parse json"))
        }
      }
      playerOptions <- toSettings(json)
    } yield {
      logger.trace(s"decrypted: $decrypted")
      (playerOptions -> tokenWithWarning._2)
    }

    result
  }


}
