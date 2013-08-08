package player.accessControl.auth

import org.bson.types.ObjectId
import org.corespring.platform.core.models.auth.AccessToken
import org.corespring.platform.core.models.error.InternalError
import play.api.libs.json.Json
import play.api.mvc.Results._
import play.api.mvc._
import player.accessControl.auth.requests.TokenizedRequest
import player.accessControl.cookies.PlayerCookieReader
import player.accessControl.models.RequestedAccess._
import player.accessControl.models._
import scala.Left
import scala.Right
import scala.Some


/** An implementation of TokenizedRequestActionBuilder that grants access based on the requested access and render options.
  * RequestedAccess is defined by the controllers. It defines the type of access that the controller will need.
  * RenderOptions is set as a session cookie it defines what rendering options have been granted to the session.
  * Using these two models we can make a decision on whether this request should be granted access.
  *
  */
abstract class CheckSession extends TokenizedRequestActionBuilder[RequestedAccess] with PlayerCookieReader {

  def ValidatedAction(ra: RequestedAccess)(block: (TokenizedRequest[AnyContent]) => Result): Action[AnyContent] =
    ValidatedAction(play.api.mvc.BodyParsers.parse.anyContent)(ra)(block)

  def ValidatedAction(p: BodyParser[AnyContent])(ra: RequestedAccess)(block: (TokenizedRequest[AnyContent]) => Result): Action[AnyContent] =
    Action {
      request =>
        val options = renderOptions(request)

        /** Once access has been granted invoke the block and pass in a TokenizedRequest */
        def invokeBlock: Result = orgIdFromCookie(request) match {
          case Some(orgId) => {
            AccessToken.getTokenForOrgById(new ObjectId(orgId)) match {
              case Some(token) => block(TokenizedRequest(token.tokenId, request))
              case _ => BadRequest("Can't find access token for Org")
            }
          }
          case _ => BadRequest("Can't find org id")
        }

        options.map {
          o =>
            grantAccess(activeMode(request), ra, o) match {
              case Right(true) => invokeBlock
                //TODO: ApiError dependency?
              case Right(false) => Unauthorized(Json.toJson("you can't access the items"))
              case Left(e) => Unauthorized(Json.toJson(e.clientOutput))
            }
        }.getOrElse(BadRequest("Couldn't find options"))

    }

  /** Grant access for this request?
    */
  def grantAccess(activeMode: Option[Mode.Mode], a: RequestedAccess, o: RenderOptions): Either[InternalError, Boolean]

}
