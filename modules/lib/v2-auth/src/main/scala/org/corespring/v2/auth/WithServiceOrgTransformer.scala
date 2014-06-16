package org.corespring.v2.auth

import org.bson.types.ObjectId
import org.corespring.platform.core.models.Organization
import org.corespring.v2.auth.services.OrgService
import org.slf4j.{Logger, LoggerFactory}
import play.api.mvc.RequestHeader
import scalaz.{Failure, Success, Validation}

object WithServiceOrgTransformer{
  def noOrgId(orgId:ObjectId) = s"No org for orgId $orgId"
  def noDefaultCollection(orgId:ObjectId) = s"No default collection for org ${orgId}"
}

trait OrgTransformer[B]{
  def apply(rh: RequestHeader): Validation[String, B]
}

private[auth] trait WithServiceOrgTransformer[B] extends OrgTransformer[B]{
  def orgService: OrgService

  def data(rh: RequestHeader, org: Organization, defaultCollection: ObjectId): B

  lazy val logger: Logger = LoggerFactory.getLogger("v2.auth.WithOrgTransformer")

  def headerToOrgId(rh: RequestHeader): Validation[String, ObjectId]

  def apply(rh: RequestHeader): Validation[String, B] = {
    import scalaz.Scalaz._
    import WithServiceOrgTransformer._

    for {
      orgId <- headerToOrgId(rh)
      org <- orgService.org(orgId).toSuccess(noOrgId(orgId))
      dc <- orgService.defaultCollection(org).toSuccess(noDefaultCollection(org.id))
    } yield {
      data(rh, org, dc)
    }
  }
}

