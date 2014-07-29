package org.corespring.v2.auth.identifiers

import org.bson.types.ObjectId
import org.corespring.v2.errors.Errors.compoundError
import org.corespring.v2.errors.V2Error
import org.corespring.v2.log.V2LoggerFactory
import org.slf4j.LoggerFactory
import play.api.http.Status._
import play.api.mvc.RequestHeader

import scalaz.{ Failure, Validation }

object WithRequestIdentitySequence {
  val errorMessage = "Failed to identify an Organization from the request"
}
trait WithRequestIdentitySequence[B] extends RequestIdentity[B] with HeaderAsOrgId {

  lazy val logger = V2LoggerFactory.getLogger("auth.WithRequestIdentitySequence")

  def identifiers: Seq[OrgRequestIdentity[B]]

  override def headerToOrgId(rh: RequestHeader): Validation[V2Error, ObjectId] = {

    val out: Seq[Validation[V2Error, ObjectId]] = identifiers.map { tf =>
      logger.trace(s"header to org with ${tf.getClass.getSimpleName}")
      tf.headerToOrgId(rh)
    }

    out.find(_.isSuccess).getOrElse {

      logger.trace(s"header to org id result: ${out.mkString(",")}")

      Failure(compoundError(
        WithRequestIdentitySequence.errorMessage,
        out.filter(_.isFailure).map(_.toEither).map(_.left.get),
        UNAUTHORIZED))
    }
  }

  override def apply(rh: RequestHeader): Validation[V2Error, B] = {

    val out: Seq[Validation[V2Error, B]] = identifiers.map { tf =>
      tf(rh)
    }

    val result: Validation[V2Error, B] = identifiers.map(_(rh)).find(_.isSuccess).getOrElse {
      logger.trace(s"identify result: ${out.mkString(",")}")

      Failure(
        compoundError(
          WithRequestIdentitySequence.errorMessage,
          out.filter(_.isFailure).map(_.toEither).map(_.left.get),
          UNAUTHORIZED))
    }

    logger.trace(s"identification result $result")

    result
  }
}

