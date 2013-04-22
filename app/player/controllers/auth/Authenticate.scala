package player.controllers.auth

import play.api.mvc.{Result, BodyParser, Action}
import player.models.TokenizedRequest

trait Authenticate[A] {
  def OrgAction(p: BodyParser[A])(block: TokenizedRequest[A] => Result): Action[A]

  def OrgAction(block: TokenizedRequest[A] => Result): Action[A]
}
