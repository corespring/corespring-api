package controllers.auth

import play.api.mvc._
import controllers.auth.requests.TokenizedRequest

trait ActionBuilder[ACCESS_DESCRIPTION, CONTENT <: AnyContent, REQUEST <: Request[CONTENT]] {
  def ValidatedAction(access:ACCESS_DESCRIPTION)(block: REQUEST => Result) : Action[CONTENT]
  def ValidatedAction(p:BodyParser[CONTENT])(access:ACCESS_DESCRIPTION)(block: REQUEST => Result) : Action[CONTENT]
}


trait TokenizedRequestActionBuilder[ACCESS] extends ActionBuilder[ACCESS,AnyContent, TokenizedRequest[AnyContent]]
