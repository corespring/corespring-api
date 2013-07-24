package player.accessControl.auth


import api.ApiError
import controllers.InternalError
import controllers.auth.TokenizedRequestActionBuilder
import controllers.auth.requests.TokenizedRequest
import models.auth.AccessToken
import org.bson.types.ObjectId
import play.api.libs.json.Json
import play.api.mvc.Results._
import play.api.mvc._
import player.accessControl.cookies.PlayerCookieReader
import player.accessControl.models.RequestedAccess._
import player.accessControl.models._
import scala.Left
import scala.Right
import scala.Some
import scala.collection.mutable
import org.apache.commons.collections.Buffer

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
              case Right(false) => Unauthorized(Json.toJson(ApiError.InvalidCredentials(Some("you can't access the items"))))
              case Left(e) => Unauthorized(Json.toJson(ApiError.InvalidCredentials(e.clientOutput)))
            }
        }.getOrElse({
          request.cookies.get("PLAY_SESSION") match {
            case Some(cookie) => BadRequest("Couldn't find options")
            case None => {
              if (isSafari(request)) {
                if (request.path.endsWith("session/redirect")) {
                  val referer = request.queryString.get("referer").getOrElse(Seq[String]("")).head
                  Ok("""<script src='/player.js?""" + request.rawQueryString + """'></script><script type="text/javascript">window.location='""" + referer + "';</script>").as("text/html")
                } else {
                  Ok("""<script type="text/javascript">top.location.href = 'session/redirect?""" + request.rawQueryString + "&referer=" + request.headers("Referer") + """';</script>""").as("text/html")
                }
              } else {
                BadRequest("Couldn't find options")
              }
            }
          }

        })

    }

  def isSafari(request: Request[AnyContent]) = {
    request.headers("user-agent").equals("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_3) AppleWebKit/536.29.13 (KHTML, like Gecko) Version/6.0.4 Safari/536.29.13")
  }

  /** Grant access for this request?
    */
  def grantAccess(activeMode: Option[Mode.Mode], a: RequestedAccess, o: RenderOptions): Either[InternalError, Boolean]

}
