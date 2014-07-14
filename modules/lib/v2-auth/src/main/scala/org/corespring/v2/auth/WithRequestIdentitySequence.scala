package org.corespring.v2.auth

import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import play.api.mvc.RequestHeader
import scalaz.{ Failure, Success, Validation }

trait WithRequestIdentitySequence[B] extends RequestIdentity[B] with HeaderAsOrgId {

  lazy val logger = LoggerFactory.getLogger("v2.auth.RequestIdentitySequence")

  def identifiers: Seq[OrgRequestIdentity[B]]

  override def headerToOrgId(rh: RequestHeader): Validation[String, ObjectId] = {
    identifiers.foldLeft[Validation[String, ObjectId]](Failure("no object id in header")) { (acc, tf) =>
      acc match {
        case Success(id) => Success(id)
        case Failure(e) => tf.headerToOrgId(rh)
      }
    }
  }

  override def apply(rh: RequestHeader): Validation[String, B] = {
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

