package controllers.auth

import play.api.mvc._
import controllers.InternalError
import api.ApiError
import scala.Left
import scala.Right
import scala.Some
import play.api.libs.json.Json
import encryption.AESCrypto
import player.controllers.auth.Authenticate
import player.models.TokenizedRequest
import models.auth.AccessToken
import org.bson.types.ObjectId

object BaseRender extends Results with BodyParsers with Authenticate[AnyContent]{
  val RendererHeader = "Renderer"
  val Delimeter = "-"
  val ClientId = "clientId"
  val Options = "options"

  case class RenderRequest[A](ctx: RendererContext, r:Request[A]) extends WrappedRequest(r){
    def reencrypt:String =
      ctx.apiClient.clientId+Delimeter+AESCrypto.encrypt(Json.toJson(ctx.options).toString(),ctx.apiClient.clientSecret)
  }

  /**
   * Returns the renderer key either from json body, from the query string, or from the session
   *
   * @param request
   * @tparam A
   * @return
   */
  private def keyFromRequest[A](request: Request[A]): Either[ApiError, RendererContext] = {
    request.body match {
      case jsbody:AnyContentAsJson => Right(Json.fromJson[RendererContext](jsbody.json))
      case _ => keyFromQuery(request) match {
        case Right(Some(rc)) => Right(rc)
        case Left(error) => Left(error)
        case Right(None) => keyFromSession(request) match {
          case Right(Some(rc)) => Right(rc)
          case Left(error) => Left(error)
          case Right(None) => Left(ApiError.MissingCredentials)
        }
      }
    }
  }
  private def keyFromQuery[A](request:Request[A]):Either[ApiError,Option[RendererContext]] = {
    request.queryString.get(ClientId).map(_.head) match {
      case Some(clientId) => request.queryString.get(Options).map(_.head) match {
        case Some(encrypted) => RendererContext.decryptContext(clientId,encrypted) match {
          case Some(ctx) => Right(Some(ctx))
          case None => Left(ApiError.ParseKey)
        }
        case None => Left(ApiError.NoOptionsProvided)
      }
      case None => Right(None)
    }
  }
  private def keyFromSession[A](request:Request[A]):Either[ApiError,Option[RendererContext]] = {
    request.session.get(RendererHeader) match {
      case Some(renderValue) => renderValue.split(Delimeter) match {
        case Array(clientId,encrypted) => RendererContext.decryptContext(clientId,encrypted) match {
          case Some(ctx) => Right(Some(ctx))
          case None => Left(ApiError.ParseKey)
        }
        case  _ => Left(ApiError.InvalidKeyType)
      }
      case None => Right(None)
    }
  }

  /**
   * A helper method to create an action for the render calls
   *
   * @param p - the body parser
   * @param f - the method that gets executed if the credentials are ok
   * @tparam A - the type of the body parser (eg: AnyContent)
   * @return a Result or BadRequest if the credentials are invalid
   */
  def RenderAction[A](p: BodyParser[A])(f: RenderRequest[A] => Result) = {
    Action(p) { request =>
      keyFromRequest(request) match {
        case Left(error) => BadRequest(Json.toJson(error))
        case Right(ctx) => f(RenderRequest(ctx,request))
      }
    }
  }
  /**
   * A helper method to create an action for render calls
   *
   * @param f - the method that gets executed if the credentials are ok
   * @return a Result or BadRequest if the credentials are invalid
   */
  def RenderAction(f: RenderRequest[AnyContent] => Result): Action[AnyContent] = {
    RenderAction(parse.anyContent)(f)
  }
  private def hasAccess(ra:RequestedAccess, ro:RenderOptions):Either[InternalError,Unit] = {
    val itemIdCheck:Either[InternalError,Unit] = if (ro.itemId != Some("*")) ra.itemId match {
      case Some(ContentRequest(id,p)) =>
        if ((p.value&Permission.Read.value)==Permission.Read.value && Some(id.toString) == ro.itemId) Right(())
        else Left(InternalError("cannot access item",addMessageToClientOutput = true))
      case _ => Right(())
    } else Right(())
    val sessionIdCheck:Either[InternalError,Unit] = if (ro.sessionId != Some("*")) ra.sessionId match {
      case Some(ContentRequest(id,p)) =>
        if ((p.value&Permission.Read.value)==Permission.Read.value && Some(id.toString) == ro.sessionId) Right(())
        else Left(InternalError("cannot access session",addMessageToClientOutput = true))
      case _ => Right(())
    } else Right(())
    val assessmentIdCheck:Either[InternalError,Unit] = if (ro.assessmentId != Some("*")) ra.assessmentId match {
      case Some(ContentRequest(id,p)) =>
        if ((p.value&Permission.Read.value)==Permission.Read.value && Some(id.toString) == ro.assessmentId) Right(())
        else Left(InternalError("cannot access assessment",addMessageToClientOutput = true))
      case _ => Right(())
    } else Right(())
    itemIdCheck match {
      case Right(_) => sessionIdCheck match {
        case Right(_) => assessmentIdCheck match {
          case Right(_) => Right(())
          case Left(e) => Left(e)
        }
        case Left(e) => Left(e)
      }
      case Left(e) => Left(e)
    }
  }


  def OrgAction(ra: RequestedAccess)(block: (TokenizedRequest[AnyContent]) => Result): Action[AnyContent] =
    OrgAction(play.api.mvc.BodyParsers.parse.anyContent)(ra)(block)

  def OrgAction(p:BodyParser[AnyContent])(ra: RequestedAccess)(block: (TokenizedRequest[AnyContent]) => Result): Action[AnyContent] =
    Action{ request =>
      val options = request.session.get("renderOptions").map{ json => Json.parse(json).as[RenderOptions] }

      def invokeBlock : Result = request.session.get("orgId") match {
        case Some(orgId) => {
          AccessToken.getTokenForOrgById(new ObjectId(orgId)) match {
            case Some(token) => block(TokenizedRequest(token.tokenId,request))
            case _ => BadRequest("Can't find access token for Org")
          }
        }
        case _ => BadRequest("Can't find org id")
      }

      options.map{ o =>
        hasAccess(ra,o) match {
          case Right(_) => invokeBlock
          case Left(e) => Unauthorized(Json.toJson(ApiError.InvalidCredentials(e.clientOutput)))
        }
      }.getOrElse(BadRequest("Couldn't find options"))

    }

}
