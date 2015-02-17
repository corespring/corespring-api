package org.corespring.v2.auth

import org.bson.types.ObjectId
import org.corespring.platform.core.models.item.Item
import org.corespring.v2.auth.SessionAuth.Session
import org.corespring.v2.errors.V2Error
import play.api.libs.json.JsValue

import scalaz.Validation
object SessionAuth {
  type Session = JsValue
}

trait SessionAuth[IDENTITY, CONTENT] {
  def loadForRead(sessionId: String)(implicit identity: IDENTITY): Validation[V2Error, (Session, CONTENT)]
  def loadForWrite(sessionId: String)(implicit identity: IDENTITY): Validation[V2Error, (Session, CONTENT)]
  def canCreate(itemId: String)(implicit identity: IDENTITY): Validation[V2Error, Boolean]
  def saveSessionFunction(implicit identity: IDENTITY): Validation[V2Error, (String, Session) => Option[Session]]
  def create(session: Session)(implicit identity: IDENTITY): Validation[V2Error, ObjectId]
}
