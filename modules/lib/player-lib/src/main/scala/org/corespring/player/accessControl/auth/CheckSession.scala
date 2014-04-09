package org.corespring.player.accessControl.auth

import org.bson.types.ObjectId
import org.corespring.platform.core.models.auth.AccessToken
import org.corespring.platform.core.models.error.InternalError
import org.corespring.player.accessControl.auth.requests.TokenizedRequest
import org.corespring.player.accessControl.cookies.PlayerCookieReader
import org.corespring.player.accessControl.models.RequestedAccess.Mode
import org.corespring.player.accessControl.models.{ RenderOptions, RequestedAccess }
import play.api.libs.json.{JsString, Json}
import play.api.mvc.Results._
import play.api.mvc._
import scala.concurrent.{ExecutionContext, Future}
import scalaz.{Failure, Success}


/**
 * An implementation of TokenizedRequestActionBuilder that grants access based on the requested access and render options.
 * RequestedAccess is defined by the controllers. It defines the type of access that the controller will need.
 * RenderOptions is set as a session cookie or is contained in encrpypted options in the request params
 * it defines what rendering options have been granted to the session.
 * Using these two models we can make a decision on whether this request should be granted access.
 *
 */
abstract class CheckSession extends TokenizedRequestActionBuilder[RequestedAccess] with PlayerCookieReader with AuthParams {

  import ExecutionContext.Implicits.global

  override def ValidatedAction(ra: RequestedAccess)(block: (TokenizedRequest[AnyContent]) => Future[SimpleResult]): Action[AnyContent] =
    ValidatedAction(play.api.mvc.BodyParsers.parse.anyContent)(ra)(block)

  override def ValidatedAction(p: BodyParser[AnyContent])(ra: RequestedAccess)(block: (TokenizedRequest[AnyContent]) => Future[SimpleResult]): Action[AnyContent] =
    Action.async {
      request =>

        val orgIdOption = orgIdFromCookie(request) match {
          case Some(orgId) => Some(orgId)
          case None => {
            getApiClientFromParams(request) match {
              case Failure(msg) => None
              case Success(client) => {
                Some(client.orgId.toString)
              }
            }

          }
        }

      /** Once access has been granted invoke the block and pass in a TokenizedRequest */
        def invokeBlock: Future[SimpleResult] = orgIdOption match {
          case Some(orgId) => {
            AccessToken.getTokenForOrgById(new ObjectId(orgId)) match {
              case Some(token) => block(TokenizedRequest(token.tokenId, request))
              case _ => Future(BadRequest("Can't find access token for Org"))
            }
          }
          case _ => Future(BadRequest("no org id"))
        }

        val options = renderOptionsFromCookie(request) match {
          case Some(options) => Some(options)
          case None => getOptionsFromParams(request)
        }


        options.map {
          o =>
            grantAccess(Option(o.mode), ra, o) match {
              case Right(true) => invokeBlock
              case Right(false) => Future(Unauthorized(Json.obj( "error" -> JsString("you can't access the items"))))
              case Left(e) => Future(Unauthorized(Json.toJson(e.clientOutput)))
            }
        }.getOrElse(Future(Unauthorized(Json.obj("error" -> JsString("no render options")))))
    }

  /**
   * Grant access for this request?
   */
  def grantAccess(activeMode: Option[Mode.Mode], a: RequestedAccess, o: RenderOptions): Either[InternalError, Boolean]

}
