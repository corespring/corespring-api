package org.corespring.v2.auth.identifiers

import org.bson.types.ObjectId
import org.corespring.models.auth.{ AccessToken, ApiClient }
import org.corespring.models.{ Organization, User }
import org.corespring.services.OrganizationService
import org.corespring.services.auth.{ AccessTokenService, ApiClientService }
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

  override def apiClientId: Option[ObjectId] = Some(input.apiClientId)
}

class TokenOrgIdentity(
  tokenService: AccessTokenService,
  val orgService: OrganizationService,
  apiClientService: ApiClientService)
  extends OrgAndOptsIdentity[AccessToken]
  with TokenReader {

  override lazy val logger = Logger(classOf[TokenOrgIdentity])
  override val name = "access-token-in-query-string"

  override def toInput(rh: RequestHeader): Validation[V2Error, Input[AccessToken]] = {
    def onToken(token: String) = for {
      t <- tokenService.findByTokenId(token).toSuccess(generalError(s"can't find token with id: $token"))
    } yield TokenIdentityInput(t)

    def onError(e: String) = Failure(if (e == "Invalid token") invalidToken(rh) else noToken(rh))
    logger.trace(s"getToken from request")
    getToken[String](rh, "Invalid token", "No token").fold(onError, onToken)
  }

  override def toOrgAndUser(i: Input[AccessToken]): Validation[V2Error, (Organization, Option[User])] = {
    orgService.findOneById(i.input.organization).toSuccess(cantFindOrgWithId(i.input.organization)).map { o =>
      o -> None
    }
  }

  def headerToOrgAndApiClient(rh: RequestHeader): Validation[V2Error, (Organization, ApiClient)] = for {
    accessToken <- toInput(rh).map(_.input)
    apiClient <- apiClientService.findByClientId(accessToken.apiClientId.toString).toSuccess(cantFindApiClientWithId(accessToken.apiClientId.toString))
    org <- orgService.findOneById(accessToken.organization).toSuccess(cantFindOrgWithId(accessToken.organization))
  } yield org -> apiClient
}
