package org.corespring.v2player.integration.auth

import org.corespring.platform.core.models.item.Item
import play.api.libs.json.JsValue
import play.api.mvc.RequestHeader

import scalaz.Validation

trait SessionAuth {
  type Session = JsValue
  def loadForRead(sessionId: String)(implicit header: RequestHeader): Validation[String, (Session, Item)]
  def loadForWrite(sessionId: String)(implicit header: RequestHeader): Validation[String, (Session, Item)]
  def canCreate(itemId: String)(implicit header: RequestHeader): Validation[String, Boolean]
}
