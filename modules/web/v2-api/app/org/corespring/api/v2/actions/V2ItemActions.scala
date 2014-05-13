package org.corespring.api.v2.actions

import play.api.mvc.{Action, Result, WrappedRequest, Request}
import org.bson.types.ObjectId

trait V2ItemActions {

  def create[A](block: OrgRequest[A] => Result) : Action[A]
}

case class OrgRequest[A](r : Request[A], orgId: ObjectId, defaultCollection: ObjectId) extends WrappedRequest[A](r)
