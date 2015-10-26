package org.corespring.models.error

case class CorespringInternalError(message: String, e: Option[Throwable] = None) {
  def clientOutput = Some(message)
}

object CorespringInternalError {
  def apply(message: String, e: Throwable): CorespringInternalError = {
    CorespringInternalError(message, Some(e))
  }
}