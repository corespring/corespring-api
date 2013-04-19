package controllers.auth

import play.api.mvc._
import api.ApiError
import play.api.libs.json.Json
import scala.Left
import play.api.mvc.AnyContentAsJson
import scala.Right
import scala.Some
import play.api.mvc.BodyParsers.parse
import encryption.AESCrypto

object RenderOptionsAuth {
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
}
