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

  val allowExpiredTokens = false

  override def toInput(request: RequestHeader): Validation[V2Error, Input[AccessToken]] = {
    getToken[String](request, "Invalid token", "No token") match {
      case Left(errorMessage) => Failure(errorMessage match {
        case "Invalid token" => invalidToken(request)
        case _ => noToken(request)
      })
      case Right(token) => tokenService.findByTokenId(token) match {
        case Some(accessToken) => {
          logger.debug(s"function=toInput, token=$accessToken, expired=${accessToken.isExpired}")
          accessToken.isExpired match {
            case false => Success(TokenIdentityInput(accessToken))
            case true => {
              logger.error(s"function=toInput, accessToken=$accessToken - token is expired")
              allowExpiredTokens match {
                case false => Failure(expiredToken(request))
                case true => Success(TokenIdentityInput(accessToken))
              }
            }
          }
        }
        case None => Failure(generalError(s"can't find token with id: $token"))
      }
    }
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
