package org.corespring.v2.api.actions

import org.bson.types.ObjectId
import play.api.mvc._

import scala.concurrent.Future

trait V2ApiActions[A] {

  def orgAction(bp: BodyParser[A])(block: OrgRequest[A] => Future[SimpleResult]): Action[A]
}

case class OrgRequest[A](r: Request[A], orgId: ObjectId, defaultCollection: ObjectId) extends WrappedRequest[A](r)
