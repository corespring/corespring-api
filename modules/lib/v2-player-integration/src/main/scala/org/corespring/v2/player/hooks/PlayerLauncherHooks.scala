package org.corespring.v2.player.hooks

import org.corespring.container.client.hooks.{ PlayerJs, PlayerLauncherHooks => ContainerPlayerLauncherHooks }
import org.corespring.services.UserService
import org.corespring.v2.auth.LoadOrgAndOptions
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.Errors.compoundError
import play.api.Logger
import play.api.mvc._

import scala.concurrent.Future
import scalaz._

trait PlayerLauncherHooks extends ContainerPlayerLauncherHooks with LoadOrgAndOptions {

  lazy val logger = Logger(classOf[PlayerLauncherHooks])

  def userService: UserService

  override def playerJs(implicit header: RequestHeader): Future[PlayerJs] = load(header)

  override def editorJs(implicit header: RequestHeader): Future[PlayerJs] = load(header)

  override def catalogJs(implicit header: RequestHeader): Future[PlayerJs] = load(header)

  private def load(implicit header: RequestHeader): Future[PlayerJs] = Future {

    logger.trace(s"load js...")
    getOrgAndOptions(header) match {
      case Success(OrgAndOpts(_, opts, _, _, _, warnings)) => {
        PlayerJs(
          opts.secure,
          header.session,
          warnings = warnings.map(w => s"${w.code}: ${w.message}"))
      }
      case Failure(error) => error match {

        case compoundError(msg, errs, _) =>
          PlayerJs(false, header.session, Seq(s"${error.errorType}: errors: ${errs.map(e => s"${e.errorType}: ${e.message}").mkString("\n")}"))
        case _ =>
          PlayerJs(false, header.session, Seq(s"${error.errorType}: ${error.message}"))

      }
    }

  }

}
