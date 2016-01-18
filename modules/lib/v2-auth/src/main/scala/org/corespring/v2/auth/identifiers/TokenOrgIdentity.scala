package org.corespring.v2.auth.identifiers

import org.corespring.models.auth.AccessToken
import org.corespring.models.{ User, Organization }
import org.corespring.services.OrganizationService
import org.corespring.services.auth.{ UpdateAccessTokenService, ApiClientService, AccessTokenService }
import org.corespring.v2.errors.Errors._
import org.corespring.v2.errors.V2Error

import play.api.Logger
import org.corespring.web.token.TokenReader
import play.api.mvc.RequestHeader

import scalaz.{ Failure, Success, Validation }
import scalaz.Scalaz._

abstract class TokenOrgIdentity[B](
  tokenService: AccessTokenService,
  val orgService: OrganizationService,
  apiClientService: ApiClientService)
  extends OrgRequestIdentity[B]
  with TokenReader {

  override lazy val logger = Logger(classOf[TokenOrgIdentity[B]])
  override val name = "access-token-in-query-string"

  override def headerToOrgAndMaybeUser(rh: RequestHeader): Validation[V2Error, (Organization, Option[User])] = {

    def updateApiClientId(t: AccessToken) = {
      logger.warn(s"Token: ${t.tokenId} has no apiClientId - adding one...")
      val client = apiClientService.getOrCreateForOrg(t.organization).toOption
      client.foreach { c =>
        val update = t.copy(apiClientId = Some(c.clientId))
        logger.info(s"Token: ${t.tokenId} has no apiClientId - adding one apiClient: ${c.clientId}")
        tokenService.asInstanceOf[UpdateAccessTokenService].update(update)
      }
    }

    def onToken(token: String) = for {
      t <- tokenService.findByTokenId(token).toSuccess(generalError(s"can't find token with id: $token"))
      _ <- Success(if (t.apiClientId.isEmpty) updateApiClientId(t))
      o <- orgService.findOneById(t.organization).toSuccess(cantFindOrgWithId(t.organization))
    } yield o -> None

    def onError(e: String) = Failure(if (e == "Invalid token") invalidToken(rh) else noToken(rh))
    logger.trace(s"getToken from request")
    getToken[String](rh, "Invalid token", "No token").fold(onError, onToken)
  }

  /** get the apiClient if available */
  override def headerToApiClientId(rh: RequestHeader): Option[String] = None

}
