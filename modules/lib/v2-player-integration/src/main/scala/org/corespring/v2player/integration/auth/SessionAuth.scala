package org.corespring.v2player.integration.auth

import scalaz.Validation

trait SessionAuth {
  def canAccessSession(sessionId: String): Validation[String, Boolean]
  def canWriteToSession(sessionId: String): Validation[String, Boolean]
}
