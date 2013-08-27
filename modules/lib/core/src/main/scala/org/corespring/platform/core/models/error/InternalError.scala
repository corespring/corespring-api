package org.corespring.platform.core.models.error

case class InternalError(message: String, e: Option[Throwable] = None) {

  play.api.Logger(this.getClass.getSimpleName).error(message)

  def clientOutput = Some(message)

}

object InternalError {
  def apply(message: String, e: Throwable): InternalError = {
    InternalError(message, Some(e))
  }
}
