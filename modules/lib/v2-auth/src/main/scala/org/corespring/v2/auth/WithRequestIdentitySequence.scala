package org.corespring.v2.auth

import play.api.mvc.RequestHeader
import scalaz.{ Failure, Success, Validation }

trait WithRequestIdentitySequence[B] extends RequestIdentity[B] {

  def identifiers: Seq[OrgRequestIdentity[B]]

  def apply(rh: RequestHeader): Validation[String, B] = {
    identifiers.foldLeft[Validation[String, B]](Failure("Failed to transform request")) { (acc, tf) =>
      acc match {
        case Success(d) => Success(d)
        case Failure(e) => {
          tf(rh)
        }
      }
    }
  }
}

