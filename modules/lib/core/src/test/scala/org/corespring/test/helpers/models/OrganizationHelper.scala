package org.corespring.test.helpers.models

import org.bson.types.ObjectId
import org.corespring.platform.core.models.Organization

object OrganizationHelper {

  import faker._

  def create(name: String = Company.name): ObjectId = Organization.insert(Organization(id = new ObjectId, name = name), None) match {
    case Right(organization) => organization.id
    case _ => throw new Exception("Failed to create organization")
  }

  def delete(organizationId: ObjectId) = Organization.delete(organizationId)

}
