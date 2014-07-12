package org.corespring.v2.api.actions

import org.corespring.v2.auth.RequestIdentity
import org.slf4j.LoggerFactory
import play.api.mvc._

import scala.concurrent.{ ExecutionContext, Future }
import scalaz.{ Failure, Success }

trait CompoundAuthenticated[A] extends V2ApiActions[A] with Controller {

  def orgTransformer: RequestIdentity[OrgRequest[A]]

  implicit def ec: ExecutionContext

  lazy val logger = LoggerFactory.getLogger("v2Api.CompoundAuthenticated")

  override def orgAction(bp: BodyParser[A])(block: (OrgRequest[A]) => Future[SimpleResult]): Action[A] = Action.async(bp) { request: Request[A] =>

    logger.trace("orgAction")

    val result: Future[Future[SimpleResult]] = Future {

      orgTransformer(request) match {
        case Success(or) => block(or)
        case Failure(msg) => Future(Unauthorized(msg))
      }
    }
    result.flatMap(identity)
  }
}
