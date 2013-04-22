package player.controllers.auth

import play.api.mvc.{Result, BodyParser, Action}
import player.models.TokenizedRequest
import controllers.auth.RequestedAccess

trait Authenticate[A] {
  //def OrgAction(p: BodyParser[A])(block: TokenizedRequest[A] => Result): Action[A]
  //def OrgAction(block: TokenizedRequest[A] => Result): Action[A]

  def OrgAction(access:RequestedAccess)(block: TokenizedRequest[A] => Result) : Action[A]
  def OrgAction(p:BodyParser[A])(access:RequestedAccess)(block: TokenizedRequest[A] => Result) : Action[A]
}
