package org.corespring.v2.auth

import play.api.mvc.RequestHeader
import scalaz.{ Failure, Success, Validation }

trait WithOrgTransformerSequence[B] extends OrgTransformer[B] {

  def transformers: Seq[WithServiceOrgTransformer[B]]

  def apply(rh: RequestHeader): Validation[String, B] = {
    transformers.foldLeft[Validation[String, B]](Failure("Failed to transform request")) { (acc, tf) =>
      acc match {
        case Success(d) => Success(d)
        case Failure(e) => {
          tf(rh)
        }
      }
    }
  }
}

