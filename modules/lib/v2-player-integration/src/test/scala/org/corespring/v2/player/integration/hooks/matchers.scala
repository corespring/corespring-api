package org.corespring.v2.player.integration.hooks

import java.util.concurrent.TimeUnit

import org.specs2.matcher.{ Expectable, Matcher }

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, Future }

private[hooks] abstract class StatusMatcher[R](expectedStatus: Int, body: String) extends Matcher[R] {

  def toValidation(in: R): Either[(Int, String), _]

  def apply[S <: R](s: Expectable[S]) = {

    val testResult = toValidation(s.value)

    def callResult(success: Boolean) = result(success, s"${testResult} matches $expectedStatus & $body", s"${testResult} doesn't match $expectedStatus & $body", s)

    testResult match {
      case Left((code, msg)) => callResult(code == expectedStatus && msg == body)
      case Right(_) => callResult(false)
    }
  }
}

private[v2] case class beErrorCodeMessage[D](expectedStatus: Int, body: String) extends StatusMatcher[Either[(Int, String), _]](expectedStatus, body) {
  override def toValidation(in: Either[(Int, String), _]): Either[(Int, String), _] = in
}

private[v2] case class beFutureErrorCodeMessage[D](expectedStatus: Int, body: String) extends StatusMatcher[Future[Either[(Int, String), _]]](expectedStatus, body) {
  override def toValidation(in: Future[Either[(Int, String), _]]): Either[(Int, String), _] = {
    Await.result(in, Duration(2, TimeUnit.SECONDS))
  }
}

