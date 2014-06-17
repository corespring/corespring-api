package org.corespring.v2player.integration.auth

import play.api.mvc.RequestHeader

import scalaz.Validation

trait ItemAuth {

  def canAccessItem(itemId: String)(implicit header: RequestHeader): Validation[String, Boolean]
  def canWriteItem(itemId: String)(implicit header: RequestHeader): Validation[String, Boolean]
}
