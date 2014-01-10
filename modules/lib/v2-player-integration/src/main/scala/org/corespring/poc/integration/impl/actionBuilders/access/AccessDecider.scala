package org.corespring.poc.integration.impl.actionBuilders.access

import play.api.mvc.{AnyContent, Request}

case class AccessResult(allowed: Boolean, msgs : Seq[String])

trait AccessDecider {
  def accessForItemId(itemId : String) : AccessResult
  def accessForSessionId(sessionId : String, request: Request[AnyContent]) : AccessResult
}