package controllers.auth

import play.api.mvc._
import controllers.InternalError
import api.ApiError
import scala.Left
import scala.Right
import scala.Some
import play.api.libs.json.Json

trait BaseRender extends Controller{
  val RendererHeader = "Renderer"
  val Bearer = "Bearer"
  val Space = " "
  val RenderKey = "rkey"

  case class RenderRequest[A](ctx: RendererContext, r:Request[A], key:String) extends WrappedRequest(r)

  /**
   * Returns the renderer key either from the Play session (with key rkey) or from the Authorization header
   * in the form of "Bearer some_key"
   *
   * @param request
   * @tparam A
   * @return
   */
  def keyFromRequest[A](request: Request[A]): Either[ApiError, String] = {
    request.queryString.get(RenderKey).map(seq => Right(seq.head)).getOrElse {
      request.session.get(RenderKey).map(Right(_)).getOrElse {
        request.headers.get(RendererHeader) match {
          case Some(value) =>
            value.split(Space) match {
              case Array(Bearer, key) => Right(key)
              case _ => Left(ApiError.InvalidKeyType)
            }
          case _ => Left(ApiError.MissingCredentials)
        }
      }
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
        case Right(key) => {
          RendererContext.parserRendererContext(key) match {
            case Some(rc) => f(RenderRequest(rc,request,key))
            case None => BadRequest(Json.toJson(ApiError.ParseKey))
          }
        }
      }
    }
  }
}
