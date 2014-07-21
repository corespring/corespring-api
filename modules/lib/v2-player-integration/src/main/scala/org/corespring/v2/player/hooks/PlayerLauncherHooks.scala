package org.corespring.v2.player.hooks

import org.bson.types.ObjectId
import org.corespring.container.client.hooks.{ PlayerJs, PlayerLauncherHooks => ContainerPlayerLauncherHooks }
import org.corespring.platform.core.controllers.auth.SecureSocialService
import org.corespring.platform.core.services.UserService
import org.corespring.v2.auth.cookies.V2PlayerCookieWriter
import org.corespring.v2.auth.models.PlayerOptions
import org.corespring.v2.errors.Errors.compoundError
import org.corespring.v2.errors.V2Error
import org.corespring.v2.log.V2LoggerFactory
import org.slf4j.LoggerFactory
import play.api.mvc._

import scala.concurrent.Future
import scalaz._

trait PlayerLauncherHooks extends ContainerPlayerLauncherHooks with V2PlayerCookieWriter {

  lazy val logger = V2LoggerFactory.getLogger("PlayerLauncherHooks")

  def secureSocialService: SecureSocialService

  def userService: UserService

  def getOrgIdAndOptions(header: RequestHeader): Validation[V2Error, (ObjectId, PlayerOptions)]

  /** A helper method to allow you to create a new session out of the existing and a variable number of Key values pairs */
  override def sumSession(s: Session, keyValues: (String, String)*): Session = {
    keyValues.foldRight(s)((kv: (String, String), acc: Session) => acc + (kv._1, kv._2))
  }

  override def playerJs(implicit header: RequestHeader): Future[PlayerJs] = load(header)

  override def editorJs(implicit header: RequestHeader): Future[PlayerJs] = load(header)

  private def load(implicit header: RequestHeader): Future[PlayerJs] = Future {

    logger.trace(s"load js...")
    getOrgIdAndOptions(header) match {
      case Success((orgId, opts)) => {
        val newSession = sumSession(header.session, playerCookies(orgId, Some(opts)): _*)
        logger.trace(s"auth success: call player js with session: $newSession")
        /**
         * Note: setting a session cookie when loading the js is going to be deprecated,
         * Once we have url based authentication up and running.
         */
        PlayerJs(opts.secure, newSession)
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
