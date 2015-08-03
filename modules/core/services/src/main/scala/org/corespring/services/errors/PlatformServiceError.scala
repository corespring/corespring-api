package org.corespring.services.errors

abstract class PlatformServiceError(val message: String, val throwable: Option[Throwable] = None)

case class GeneralError(msg: String, t: Option[Throwable]) extends PlatformServiceError(msg, t)

object PlatformServiceError {
  def apply(message: String, e: Throwable = null): PlatformServiceError = if (e == null) {
    GeneralError(message, None)
  } else {
    GeneralError(message, Some(e))
  }
}
