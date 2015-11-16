package org.corespring.player.accessControl.auth

import play.api.mvc._
import org.corespring.player.accessControl.auth.requests.TokenizedRequest
import scala.concurrent.Future

/**
 * Base ActionBuilder.
 * Implementations can decide whether to grant access base on the ACCESS_DESCRIPTION and the REQUEST.
 * @tparam ACCESS_DESCRIPTION - A type that defines the type of access required - typically defined by controllers.
 * @tparam CONTENT - The request content type
 * @tparam REQUEST - The request type
 */
trait ActionBuilder[ACCESS_DESCRIPTION, CONTENT <: AnyContent, REQUEST <: Request[CONTENT]] {
  def ValidatedAction(access: ACCESS_DESCRIPTION)(block: REQUEST => Future[SimpleResult]): Action[CONTENT]
  def ValidatedAction(p: BodyParser[CONTENT])(access: ACCESS_DESCRIPTION)(block: REQUEST => Future[SimpleResult]): Action[CONTENT]
}

/** For a validated action pass the block a TokenizedRequest */
trait TokenizedRequestActionBuilder[ACCESS] extends ActionBuilder[ACCESS, AnyContent, TokenizedRequest[AnyContent]]