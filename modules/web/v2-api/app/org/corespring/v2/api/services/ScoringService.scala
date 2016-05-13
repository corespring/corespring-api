package org.corespring.v2.api.services

import org.corespring.v2.errors.V2Error
import play.api.libs.json.JsValue

import scala.concurrent.Future
import scalaz.Validation

case class ScoreResult(sessionId: String, result: Validation[V2Error, JsValue])

private[api] trait ScoringService[ID] {
  def scoreMultipleSessions(identity: ID)(ids: Seq[String]): Future[Seq[Validation[V2Error, ScoreResult]]]
}
