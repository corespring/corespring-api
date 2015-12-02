package org.corespring.services.salat

import scalaz.{ Failure, Success }
import scalaz.Validation

private[salat] object ValidationUtils {

  import scala.language.implicitConversions

  implicit def eitherToValidation[E, A](e: Either[E, A]): Validation[E, A] = e match {
    case Left(e) => Failure(e)
    case Right(r) => Success(r)
  }
}