package org.corespring.v2.auth.identifiers

import org.corespring.v2.errors.Errors.{ generalError, compoundError }
import org.corespring.v2.errors.V2Error
import play.api.Logger
import play.api.http.Status._
import play.api.mvc.RequestHeader

import scalaz.{ Success, Failure, Validation }

object WithRequestIdentitySequence {
  val errorMessage = "Failed to identify an Organization from the request"
  val emptySequenceErrorMessage = "No authentication mechanisms were provided - can't authenticate"
}

trait WithRequestIdentitySequence[B] extends RequestIdentity[B] {

  lazy val logger = Logger(classOf[WithRequestIdentitySequence[B]])

  def identifiers: Seq[OrgRequestIdentity[B]]

  override def apply(rh: RequestHeader): Validation[V2Error, B] = {

    import WithRequestIdentitySequence.emptySequenceErrorMessage

    val identificationResult = identifiers.foldLeft[Validation[V2Error, B]](Failure(generalError(emptySequenceErrorMessage, INTERNAL_SERVER_ERROR))) { (acc, tf) =>
      val out = acc match {
        case Success(d) => {
          logger.trace(s"identification was successful")
          Success(d)
        }
        case Failure(e) => {
          logger.trace(s"convert to identity with ${tf.name}")
          tf(rh)
        }
      }
      out
    }

    identificationResult.leftMap { e =>

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

  override def name: String = s"multiple-identifiers-in-a-sequence:(${identifiers.map(_.name).mkString(",")})"
}

