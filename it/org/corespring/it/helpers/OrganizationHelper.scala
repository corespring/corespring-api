package org.corespring.it.helpers

import org.bson.types.ObjectId
import org.corespring.models.Organization

import scalaz.Success

object OrganizationHelper {

  import faker._
  lazy val service = bootstrap.Main.orgService

  def createAndReturnOrg(name: String = Company.name): Organization = service.insert(Organization(id = new ObjectId, name = name), None) match {
    case Success(organization) => organization
    case _ => throw new Exception("Failed to create organization")
  }

  def create(name: String = Company.name): ObjectId = createAndReturnOrg(name).id

  def delete(organizationId: ObjectId) = service.delete(organizationId)

}
