package org.corespring.v2.auth

import org.corespring.platform.core.models.item.Item
import org.corespring.v2.auth.SessionAuth.Session
import org.corespring.v2.errors.V2Error
import play.api.libs.json.JsValue

import scalaz.Validation
object SessionAuth {
  type Session = JsValue
}
trait SessionAuth[A] {
  def loadForRead(sessionId: String)(implicit identity: A): Validation[V2Error, (Session, Item)]
  def loadForWrite(sessionId: String)(implicit identity: A): Validation[V2Error, (Session, Item)]
  def canCreate(itemId: String)(implicit identity: A): Validation[V2Error, Boolean]
}
