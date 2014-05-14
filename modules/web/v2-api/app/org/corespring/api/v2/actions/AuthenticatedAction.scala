package org.corespring.api.v2.actions

import play.api.mvc._
import scala.concurrent.Future
import org.corespring.platform.core.controllers.auth.{OAuthConstants, ApiRequest}
import org.corespring.platform.core.controllers.auth.ApiRequest
import scala.Some
import play.api.mvc.SimpleResult

trait AuthenticatedAction[A] {


  def auth(block:ApiRequest[A] => Future[SimpleResult]) : Action[A]
}

trait TokenReader{

  val AuthorizationHeader = "Authorization"
  val AccessToken = "access_token"
  val Bearer = "Bearer"
  val Space = " "

  def getToken[E](request:RequestHeader, invalidToken:E, noToken:E) : Either[E,String] = {

    def tokenInHeader : Option[String] = {
      request.headers.get(AuthorizationHeader).map{ h =>
         h.split(Space) match {
           case Array(Bearer, token) => Some(token)
           case _ => None
         }
      }.flatten
    }

    val out : Option[String] = request.queryString.get(AccessToken).map(_.head)
      .orElse(request.session.get(AccessToken))
      .orElse(tokenInHeader)

    out
  }
}

object TokenAuthenticated extends AuthenticatedAction[AnyContent]{


  override def auth(failed:  Unit => Future[SimpleResult], proceed: (ApiRequest[AnyContent]) => Future[SimpleResult]): Action[AnyContent] = Action.async{
    request :  Request[AnyContent] =>

      request.queryString.get("access_token").map{ token =>



      }.getOrElse(failed())
  }
}
