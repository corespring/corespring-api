package org.corespring.v2player.integration.auth

import play.api.mvc.RequestHeader

import scalaz.Validation

trait ItemAuth {
  def canCreateInCollection(collectionId: String)(implicit header: RequestHeader): Validation[String, Boolean]
  def canRead(itemId: String)(implicit header: RequestHeader): Validation[String, Boolean]
  def canWrite(itemId: String)(implicit header: RequestHeader): Validation[String, Boolean]
}
