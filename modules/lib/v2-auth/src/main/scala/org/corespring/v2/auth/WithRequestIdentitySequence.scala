package org.corespring.v2.auth

import org.bson.types.ObjectId
import org.corespring.v2.errors.Errors.{ identificationFailed, generalError }
import org.corespring.v2.errors.V2Error
import org.slf4j.LoggerFactory
import play.api.mvc.RequestHeader
import scalaz.{ Failure, Success, Validation }

trait WithRequestIdentitySequence[B] extends RequestIdentity[B] with HeaderAsOrgId {

  lazy val logger = LoggerFactory.getLogger("v2.auth.RequestIdentitySequence")

  def identifiers: Seq[OrgRequestIdentity[B]]

  override def headerToOrgId(rh: RequestHeader): Validation[V2Error, ObjectId] = {
    val out = identifiers.foldLeft[Validation[V2Error, ObjectId]](Failure(generalError("?"))) { (acc, tf) =>
      acc match {
        case Success(id) => Success(id)
        case Failure(e) => tf.headerToOrgId(rh)
      }
    }

    out.leftMap(e => identificationFailed(rh))
  }

  override def apply(rh: RequestHeader): Validation[V2Error, B] = {
    identifiers.foldLeft[Validation[V2Error, B]](Failure(generalError("?"))) { (acc, tf) =>
      val out = acc match {
        case Success(d) => {
          logger.trace(s"identity is successful")
          Success(d)
        }
        case Failure(e) => {
          logger.trace(s"convert to identity: with $tf")
          tf(rh)
        }
      }

      out.leftMap(e => identificationFailed(rh))
    }
  }
}

