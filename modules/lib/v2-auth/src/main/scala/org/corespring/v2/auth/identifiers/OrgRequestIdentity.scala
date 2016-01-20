package org.corespring.v2.auth.identifiers

import org.bson.types.ObjectId
import org.corespring.models.{ Organization, User }
import org.corespring.v2.auth.models.AuthMode.AuthMode
import org.corespring.v2.auth.models.{ OrgAndOpts, PlayerAccessSettings }
import org.corespring.v2.errors.V2Error
import org.corespring.v2.warnings.V2Warning
import play.api.Logger
import play.api.mvc.RequestHeader

import scalaz.Validation

/**
 * Turn an unknown request header into an identity so decisions can be made about the request.
 *
 * @tparam B the identity type
 */
trait RequestIdentity[B] {
  def apply(rh: RequestHeader): Validation[V2Error, B]

  def name: String
}

trait Input[T] {
  def input: T
  def playerAccessSettings: PlayerAccessSettings
  def authMode: AuthMode
  def apiClientId: Option[ObjectId]
  def warnings: Seq[V2Warning]
}

trait OrgAndOptsIdentity[T] extends RequestIdentity[OrgAndOpts] {

  lazy val logger = Logger(classOf[OrgAndOptsIdentity[T]])

  protected def toInput(rh: RequestHeader): Validation[V2Error, Input[T]]

  protected def toOrgAndUser(i: Input[T]): Validation[V2Error, (Organization, Option[User])]

  override def apply(rh: RequestHeader): Validation[V2Error, OrgAndOpts] = for {
    input <- toInput(rh)
    orgAndUser <- toOrgAndUser(input)
  } yield {
    OrgAndOpts(orgAndUser._1, input.playerAccessSettings, input.authMode, input.apiClientId.map(_.toString), orgAndUser._2, input.warnings)
  }
}

