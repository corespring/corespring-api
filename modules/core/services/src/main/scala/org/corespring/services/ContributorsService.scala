package org.corespring.services.metadata

import org.bson.types.ObjectId

trait ContributorsService {
  def contributorsForOrg(orgId: ObjectId): Seq[String]
}
