package player.controllers.auth


import api.ApiError
import controllers.InternalError
import controllers.auth.RenderOptions
import models.auth.AccessToken
import org.bson.types.ObjectId
import play.api.libs.json.Json
import play.api.mvc.Results._
import play.api.mvc._
import player.models.TokenizedRequest
import player.rendering.PlayerCookieReader
import scala.Left
import scala.Right
import scala.Some

object CheckPlayerSession extends Authenticate[AnyContent] with PlayerCookieReader {


  def OrgAction(ra: RequestedAccess)(block: (TokenizedRequest[AnyContent]) => Result): Action[AnyContent] =
    OrgAction(play.api.mvc.BodyParsers.parse.anyContent)(ra)(block)

  def OrgAction(p:BodyParser[AnyContent])(ra: RequestedAccess)(block: (TokenizedRequest[AnyContent]) => Result): Action[AnyContent] =
    Action{ request =>
      val options = renderOptions(request)

      def invokeBlock : Result = orgIdFromCookie(request) match {
        case Some(orgId) => {
          AccessToken.getTokenForOrgById(new ObjectId(orgId)) match {
            case Some(token) => block(TokenizedRequest(token.tokenId,request))
            case _ => BadRequest("Can't find access token for Org")
          }
        }
        case _ => BadRequest("Can't find org id")
      }

      options.map{ o =>
        //TODO: move this to check access
        if (o.expires == 0 || o.expires > System.currentTimeMillis()){
          grantAccess(activeMode(request),ra,o) match {
            case Right(true) => invokeBlock
            case Right(false) => Unauthorized(Json.toJson(ApiError.InvalidCredentials(Some("you can't access the items"))))
            case Left(e) => Unauthorized(Json.toJson(ApiError.InvalidCredentials(e.clientOutput)))
          }
        }else Unauthorized(Json.toJson(ApiError.ExpiredOptions))
      }.getOrElse(BadRequest("Couldn't find options"))

    }


  def grantAccess(activeMode: Option[String], a: RequestedAccess, o: RenderOptions): Either[InternalError, Boolean] = {

    def checkAccess(ifNotSpecified: Boolean, requests: (Option[ContentRequest], String => Boolean)*) = requests match {
      case Seq() => Left(InternalError("no properties specified"))
      case _ => {
        val allNone = requests.filter(_._1.isEmpty).length == requests.length

        if (allNone) {
          Left(InternalError("Nothing to test against"))
        } else {
          val result: Seq[Boolean] = requests.map {
            (tuple: (Option[ContentRequest], String => Boolean)) =>
              val (cr, accessFn) = tuple
              cr.map(c => accessFn(c.id.toString)).getOrElse(ifNotSpecified)
          }
          Right(result.foldRight(true)(_ && _))
        }
      }
    }

    val am: Option[String] = if (a.mode.isDefined) a.mode else activeMode

    am match {
      case Some(m) => {
        if (o.allowMode(m)) {
          m match {
            case RequestedAccess.PREVIEW_MODE => checkAccess(false, (a.itemId, o.allowItemId))
            case RequestedAccess.RENDER_MODE => checkAccess(false, (a.sessionId, o.allowSessionId))
            case RequestedAccess.ADMINISTER_MODE => checkAccess(true, (a.itemId, o.allowItemId), (a.sessionId, o.allowSessionId))
            case RequestedAccess.AGGREGATE_MODE => checkAccess(false, (a.itemId, o.allowItemId), (a.assessmentId, o.allowAssessmentId))
            case _ => Left(InternalError("TODO"))
          }
        } else {
          Left(InternalError("mode not allowed"))
        }
      }
      case _ => Left(InternalError("no mode specified"))
    }

  }

}
