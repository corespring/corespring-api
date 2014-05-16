package org.corespring.api.v2.actions

import play.api.mvc._
import scala.concurrent.{ ExecutionContext, Future }
import play.api.mvc.SimpleResult
import org.slf4j.LoggerFactory

trait CompoundAuthenticated[A] extends V2ApiActions[A] with Controller {

  def requestTransformers: Seq[Request[A] => Option[OrgRequest[A]]]

  implicit def ec: ExecutionContext

  lazy val logger = LoggerFactory.getLogger("v2Api.CompoundAuthenticated")

  override def orgAction(bp: BodyParser[A])(block: (OrgRequest[A]) => Future[SimpleResult]): Action[A] = Action.async(bp) { request: Request[A] =>

    logger.trace("orgAction")

    val result: Future[Future[SimpleResult]] = Future {

      val orgRequest: Option[OrgRequest[A]] = requestTransformers.foldLeft[Option[OrgRequest[A]]](None) { (acc, fn) =>
        if (acc.isDefined) {
          acc
        } else {
          fn(request)
        }
      }
      orgRequest.map(block).getOrElse(Future(Unauthorized))
    }
    result.flatMap(identity)
  }
}
