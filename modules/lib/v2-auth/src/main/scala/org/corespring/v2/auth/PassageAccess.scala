package org.corespring.v2.auth

import org.bson.types.ObjectId
import org.corespring.models.auth.Permission
import org.corespring.models.item.Passage
import org.corespring.services.OrgCollectionService
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.Errors.orgCantAccessCollection
import org.corespring.v2.errors.V2Error

import scalaz.{Success, Failure, Validation}

class PassageAccess(orgCollectionService: OrgCollectionService) extends Access[Passage, OrgAndOpts] {

  override def grant(identity: OrgAndOpts, permission: Permission, passage: Passage): Validation[V2Error, Boolean] = {
    def orgCanAccess(collectionId: String) = orgCollectionService.isAuthorized(identity.org.id, new ObjectId(collectionId), permission)

    orgCanAccess(passage.collectionId) match {
      case true => Success(true)
      case _ => Failure(orgCantAccessCollection(identity.org.id, passage.collectionId, permission.name))
    }
  }

}