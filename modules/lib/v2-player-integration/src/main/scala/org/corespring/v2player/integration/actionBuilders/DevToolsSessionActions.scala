package org.corespring.v2player.integration.actionBuilders

import play.api.mvc._
import scala.concurrent.{ Await, Future }
import java.util.concurrent.TimeUnit

import scalaz.{ Success, Validation }

/**
 * Wrap AuthenticatedSessionActions to check for DEV_TOOLS_ENABLED if not present call the underlying actions
 * @param underlying
 */
class DevToolsSessionActions(underlying: SessionAuth) extends SessionAuth {

  private def run(fn: (() => Validation[String, Boolean]))(implicit header: RequestHeader): Validation[String, Boolean] = {
    header.session.get("DEV_TOOLS_ENABLED") match {
      case Some("true") => Success(true)
      case _ => {
        fn()
      }
    }
  }

  override def read(sessionId: String): Validation[String, Boolean] = run(() => underlying.read(sessionId))

  override def createSession(itemId: String): Validation[String, Boolean] = run(() => underlying.createSession(itemId))

  override def loadPlayerForSession(sessionId: String): Validation[String, Boolean] = run(() => underlying.loadPlayerForSession(sessionId))
}
