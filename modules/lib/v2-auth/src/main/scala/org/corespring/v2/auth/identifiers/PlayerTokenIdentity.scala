package org.corespring.v2.auth.identifiers

import org.bson.types.ObjectId
import org.corespring.encryption.apiClient.ApiClientEncryptionService
import org.corespring.models.auth.ApiClient
import org.corespring.models.{ Organization, User }
import org.corespring.services.OrganizationService
import org.corespring.services.auth.ApiClientService
import org.corespring.v2.auth.identifiers.PlayerTokenIdentity.Keys
import org.corespring.v2.auth.models.AuthMode.AuthMode
import org.corespring.v2.auth.models.{ AuthMode, PlayerAccessSettings }
import org.corespring.v2.errors.Errors._
import org.corespring.v2.errors.V2Error
import org.corespring.v2.warnings.V2Warning
import org.corespring.v2.warnings.Warnings.deprecatedQueryStringParameter
import play.api.libs.json.{ JsSuccess, JsValue, JsError, Json }
import play.api.mvc.RequestHeader

import scalaz.Scalaz._
import scalaz.{ Success, Failure, Validation }

case class PlayerTokenInput(input: ApiClient, playerAccessSettings: PlayerAccessSettings, warnings: Seq[V2Warning]) extends Input[ApiClient] {

  override def authMode: AuthMode = AuthMode.ClientIdAndPlayerToken

  override def apiClientId: Option[ObjectId] = Some(input.clientId)
}

object PlayerTokenIdentity {

  object Keys {
    val apiClient = "apiClient"
    /** deprecated("Still supported but is going to be removed", "1.1") */
    val options = "options"
    val playerToken = "playerToken"
    val editorToken = "editorToken"
    val skipDecryption = "skipDecryption"
  }
}

case class PlayerTokenConfig(canSkipDecryption: Boolean)

class PlayerTokenIdentity(val orgService: OrganizationService,
  apiClientService: ApiClientService,
  apiClientEncryptionService: ApiClientEncryptionService,
  config: PlayerTokenConfig)
  extends OrgAndOptsIdentity[ApiClient] {

  override def name: String = "player-token-identity"

  override protected def toInput(rh: RequestHeader): Validation[V2Error, Input[ApiClient]] = {
    if (rh.getQueryString("apiClientId").isDefined) {
      Failure(invalidQueryStringParameter("apiClientId", Keys.apiClient))
    } else {

      def toAccessSettings(json: JsValue): Validation[V2Error, PlayerAccessSettings] = {
        PlayerAccessSettings.format.reads(json) match {
          case JsError(errs) => {
            logger.warn(s"path=${rh.path} - Error parsing PlayerAccessSettings: ${errs.mkString(", ")}")
            Failure(incorrectJsonFormat(json, Some(JsError(errs))))
          }
          case JsSuccess(s, _) => Success(s)
        }
      }

      val maybeClientId = rh.getQueryString(Keys.apiClient)
      logger.trace(s"function=headerToOrgId ${Keys.apiClient}=${maybeClientId.toString}")

      for {
        apiClientId <- maybeClientId.toSuccess(invalidQueryStringParameter("apiClientId", Keys.apiClient))
        apiClient <- apiClientService.findByClientId(apiClientId).toSuccess(cantFindApiClientWithId(apiClientId))
        encryptedSettings <- accessSettingsQueryString(rh).toSuccess(noPlayerTokenInQueryString(rh))
        decryptedOptions <- decrypt(apiClient, encryptedSettings._1, rh).toSuccess(generalError(s"Failed to decrypt"))
        json <- parseJsonString(decryptedOptions)
        accessSettings <- toAccessSettings(json)
      } yield {
        PlayerTokenInput(apiClient, accessSettings, encryptedSettings._2.toSeq)
      }
    }
  }

  protected def decrypt(apiClient: ApiClient, encrypted: String, header: RequestHeader): Option[String] = {
    val skipDecryptionRequested = header.getQueryString("skipDecryption").isDefined

    if (config.canSkipDecryption && skipDecryptionRequested) {
      Some(encrypted)
    } else {
      apiClientEncryptionService.decrypt(apiClient, encrypted)
    }
  }

  override protected def toOrgAndUser(i: Input[ApiClient]): Validation[V2Error, (Organization, Option[User])] =
    orgService.findOneById(i.input.orgId)
      .toSuccess(cantFindOrgWithId(i.input.orgId))
      .map { o => o -> None }

  protected def parseJsonString(s: String) = Validation.fromTryCatch {
    Json.parse(s)
  }.leftMap(t => invalidJson(s))

  protected def accessSettingsQueryString(rh: RequestHeader): Option[(String, Option[V2Warning])] = {
    val token = rh.getQueryString(Keys.playerToken).orElse(rh.getQueryString(Keys.editorToken))

    token
      .map(t => (t, None))
      .orElse {
        val deprecatedOptions = rh.getQueryString(Keys.options)
        deprecatedOptions.foreach { _ =>
          logger.warn(s"path=${rh.path} ${Keys.apiClient}=${rh.getQueryString(Keys.apiClient)} - Query string contains 'options' parameter. Inform client to use 'playerToken' instead.")
        }
        deprecatedOptions.map { t => (t, Some(deprecatedQueryStringParameter(Keys.options, Keys.playerToken))) }
      }
  }

}
