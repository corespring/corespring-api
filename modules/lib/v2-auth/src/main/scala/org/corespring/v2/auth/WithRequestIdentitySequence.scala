package org.corespring.v2.auth

import org.slf4j.LoggerFactory
import play.api.mvc.RequestHeader
import scalaz.{ Failure, Success, Validation }

trait WithRequestIdentitySequence[B] extends RequestIdentity[B] {

  lazy val logger = LoggerFactory.getLogger("v2.auth.RequestIdentitySequence")

  def identifiers: Seq[OrgRequestIdentity[B]]

  def apply(rh: RequestHeader): Validation[String, B] = {
    identifiers.foldLeft[Validation[String, B]](Failure("Failed to transform request")) { (acc, tf) =>
      acc match {
        case Success(d) => {
          logger.trace(s"identity is successful")
          Success(d)
        }
        case Failure(e) => {
          logger.trace(s"convert to identity: with $tf")
          tf(rh)
        }
      }
    }
  }
}

