package org.corespring.v2.auth.identifiers

import org.bson.types.ObjectId
import org.corespring.models.auth.{ AccessToken, ApiClient }
import org.corespring.models.{ Organization, User }
import org.corespring.services.OrganizationService
import org.corespring.services.auth.{ AccessTokenService, ApiClientService, UpdateAccessTokenService }
import org.corespring.v2.auth.models.AuthMode.AuthMode
import org.corespring.v2.auth.models.{ AuthMode, PlayerAccessSettings }
import org.corespring.v2.errors.Errors._
import org.corespring.v2.errors.V2Error
import org.corespring.v2.warnings.V2Warning
import org.corespring.web.token.TokenReader
import play.api.Logger
import play.api.mvc.RequestHeader

import scalaz.Scalaz._
import scalaz.{ Failure, Success, Validation }

case class TokenIdentityInput(input: AccessToken) extends Input[AccessToken] {
  override def playerAccessSettings: PlayerAccessSettings = PlayerAccessSettings.ANYTHING

  override def warnings: Seq[V2Warning] = Nil

  override def authMode: AuthMode = AuthMode.AccessToken

  override def apiClientId: Option[ObjectId] = input.apiClientId
}

class TokenOrgIdentity(
  tokenService: AccessTokenService,
  updateAccessTokenService: UpdateAccessTokenService,
  val orgService: OrganizationService,
  apiClientService: ApiClientService)
  extends OrgAndOptsIdentity[AccessToken]
  with TokenReader {

  override lazy val logger = Logger(classOf[TokenOrgIdentity])
  override val name = "access-token-in-query-string"

  override def toInput(rh: RequestHeader): Validation[V2Error, Input[AccessToken]] = {
    def onToken(token: String) = for {
      t <- tokenService.findByTokenId(token).toSuccess(generalError(s"can't find token with id: $token"))
      withApiClientId <- Success(tokenWithApiClientId(t))
    } yield TokenIdentityInput(withApiClientId)

    def onError(e: String) = Failure(if (e == "Invalid token") invalidToken(rh) else noToken(rh))
    logger.trace(s"getToken from request")
    getToken[String](rh, "Invalid token", "No token").fold(onError, onToken)
  }

  /**
   * AC-258
   * We are temporarily ensuring that all accessTokens have an associated apiClientId.
   * All new tokens have it, so this will only affect unexpired tokens what were created before this release.
   * @param t
   * @return
   */
  private def tokenWithApiClientId(t: AccessToken): AccessToken = t.apiClientId match {
    case Some(_) => t
    case None => {
      logger.warn(s"Token: ${t.tokenId} has no apiClientId - adding one...")
      apiClientService.getOrCreateForOrg(t.organization).toOption.map { c =>
        val update = t.copy(apiClientId = Some(c.clientId))
        logger.info(s"Token: ${t.tokenId} has no apiClientId - adding one apiClient: ${c.clientId}")
        updateAccessTokenService.update(update)
        update
      }.getOrElse {
        logger.error(s"Failed to get or create apiClient for org: ${t.organization}")
        t
      }
    }
  }

  override def toOrgAndUser(i: Input[AccessToken]): Validation[V2Error, (Organization, Option[User])] = {
    println(s"? $i")
    orgService.findOneById(i.input.organization).toSuccess(cantFindOrgWithId(i.input.organization)).map { o =>
      o -> None
    }
  }

  def headerToOrgAndApiClient(rh: RequestHeader): Validation[V2Error, (Organization, ApiClient)] = for {
    accessToken <- toInput(rh).map(_.input)
    apiClientId <- accessToken.apiClientId.toSuccess(generalError(s"token: ${accessToken.tokenId} has no apiClientId"))
    apiClient <- apiClientService.findByClientId(apiClientId.toString).toSuccess(cantFindApiClientWithId(apiClientId.toString))
    org <- orgService.findOneById(accessToken.organization).toSuccess(cantFindOrgWithId(accessToken.organization))
  } yield org -> apiClient
}
