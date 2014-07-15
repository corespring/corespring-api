package org.corespring.v2.auth

import org.corespring.platform.core.models.item.Item
import org.corespring.v2.errors.V2Error
import play.api.libs.json.JsValue
import play.api.mvc.RequestHeader

import scalaz.Validation

trait SessionAuth {
  type Session = JsValue
  def loadForRead(sessionId: String)(implicit header: RequestHeader): Validation[V2Error, (Session, Item)]
  def loadForWrite(sessionId: String)(implicit header: RequestHeader): Validation[V2Error, (Session, Item)]
  def canCreate(itemId: String)(implicit header: RequestHeader): Validation[V2Error, Boolean]
}
