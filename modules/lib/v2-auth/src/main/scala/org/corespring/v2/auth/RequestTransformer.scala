package org.corespring.v2.auth

import org.bson.types.ObjectId
import org.corespring.platform.core.models.Organization
import org.corespring.v2.auth.services.OrgService
import org.slf4j.Logger
import play.api.mvc.RequestHeader

private[corespring] trait WithOrgTransformer[B] {
  def orgService : OrgService
  def data(rh: RequestHeader, org: Organization, defaultCollection : ObjectId): B
  def logger : Logger
  def getOrgId(rh:RequestHeader) : Option[ObjectId]

  def apply(rh:RequestHeader) : Option[B] = {
    import scalaz.Scalaz._
    import scalaz._

    val out: Validation[String, B] = for {
      orgId <- getOrgId(rh).toSuccess("TODO................")
      org <- orgService.org(orgId).toSuccess(s"No org for user ${u}")
      dc <- orgService.defaultCollection(org).toSuccess(s"No default collection for org ${org.id}")
    } yield {
      data(rh, org, dc)
    }

    out match {
      case Success(or) => Some(or)
      case Failure(msg) => {
        logger.trace(msg)
        None
      }
    }
  }
}

