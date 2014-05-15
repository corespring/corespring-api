package org.corespring.platform.core.models.error

case class CorespringInternalError(message: String, e: Option[Throwable] = None) {

  play.api.Logger(this.getClass.getSimpleName).error(message)

  def clientOutput = Some(message)

}

object CorespringInternalError {
  def apply(message: String, e: Throwable): CorespringInternalError = {
    CorespringInternalError(message, Some(e))
  }
}
