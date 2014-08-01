package org.corespring.v2.auth.identifiers

import org.corespring.v2.errors.Errors.{ generalError, compoundError }
import org.corespring.v2.errors.V2Error
import org.corespring.v2.log.V2LoggerFactory
import play.api.http.Status._
import play.api.mvc.RequestHeader

import scalaz.{ Success, Failure, Validation }

object WithRequestIdentitySequence {
  val errorMessage = "Failed to identify an Organization from the request"
}

trait WithRequestIdentitySequence[B] extends RequestIdentity[B] {

  lazy val logger = V2LoggerFactory.getLogger("auth.WithRequestIdentitySequence")

  def identifiers: Seq[OrgRequestIdentity[B]]

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

      out.leftMap { e =>

        logger.trace(s"Building compound error - rerun all identifiers")
        val errs: Seq[Validation[V2Error, B]] = identifiers.distinct.map { tf =>
          tf(rh)
        }

        compoundError(
          WithRequestIdentitySequence.errorMessage,
          errs.filter(_.isFailure).map(_.toEither).map(_.left.get),
          UNAUTHORIZED)
      }
    }
  }

}

