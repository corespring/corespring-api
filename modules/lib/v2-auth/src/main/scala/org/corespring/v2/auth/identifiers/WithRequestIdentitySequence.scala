package org.corespring.v2.auth.identifiers

import org.corespring.v2.errors.Errors.{ generalError, compoundError }
import org.corespring.v2.errors.V2Error
import play.api.Logger
import play.api.http.Status._
import play.api.mvc.RequestHeader

import scalaz.{ Failure, Validation }

object WithRequestIdentitySequence {
  val errorMessage = "Failed to identify an Organization from the request"
  val emptySequenceErrorMessage = "No authentication mechanisms were provided - can't authenticate"
}

trait WithRequestIdentitySequence[B] extends RequestIdentity[B] {

  override lazy val name = s"multiple-identifiers-in-a-sequence:(${identifiers.map(_.name).mkString(", ")})"

  lazy val logger = Logger(classOf[WithRequestIdentitySequence[B]])

  def identifiers: Seq[OrgRequestIdentity[B]]

  override def apply(rh: RequestHeader): Validation[V2Error, B] = {

    import WithRequestIdentitySequence.emptySequenceErrorMessage

    if (identifiers.nonEmpty) {

      /**
       * return either Seq(Success()) or Seq(Failure(), Failure()...)
       */
      val t = identifiers.foldLeft[Seq[Validation[V2Error, B]]](Seq.empty) { (acc, tf) =>
        acc.find(_.isSuccess).map { success =>
          Seq(success)
        }.getOrElse {
          acc :+ tf(rh)
        }
      }

      t.find(_.isSuccess).headOption.getOrElse {
        val e = compoundError(
          WithRequestIdentitySequence.errorMessage,
          t.filter(_.isFailure).map(_.swap.toOption.get),
          UNAUTHORIZED)
        Failure(e)
      }
    } else {
      Failure(generalError(emptySequenceErrorMessage, INTERNAL_SERVER_ERROR))
    }
  }
}
