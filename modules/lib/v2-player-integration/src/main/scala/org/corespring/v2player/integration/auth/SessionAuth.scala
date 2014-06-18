package org.corespring.v2player.integration.auth

import play.api.mvc.RequestHeader

import scalaz.Validation

trait SessionAuth {
  def canRead(sessionId: String)(implicit header: RequestHeader): Validation[String, Boolean]
  def canWrite(sessionId: String)(implicit header: RequestHeader): Validation[String, Boolean]
  def canCreate(itemId: String)(implicit header: RequestHeader): Validation[String, Boolean]
}
