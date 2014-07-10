package org.corespring.api.v2.services

import org.bson.types.ObjectId
import org.corespring.api.v2.errors.Errors._
import org.corespring.api.v2.errors.V2ApiError
import org.corespring.platform.core.models.Organization
import org.corespring.platform.core.models.auth.Permission
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.organization.OrganizationService

import scalaz.Scalaz._
import scalaz.{ Failure, Success, Validation }

trait ItemPermissionService extends PermissionService[Organization, Item] {

  def organizationService: OrganizationService

  override def create(client: Organization, item: Item): Validation[V2ApiError, Item] = for {
    id <- item.collectionId.toSuccess(noCollectionIdInItem(item.id))
    contentCollection <- client.contentcolls.find(_.collectionId.toString == id).toSuccess(orgDoesntReferToCollection(client.id, id))
    collPermission <- Permission.fromLong(contentCollection.pval).toSuccess(invalidPval(contentCollection.pval, id, client.id))
    accessibleItem <- if (collPermission.has(Permission.Write)) Success(item) else Failure(insufficientPermission(contentCollection.pval, Permission.Write))
  } yield {
    accessibleItem
  }

  override def get(client: Organization, item: Item): Validation[V2ApiError, Item] = for {
    id <- item.collectionId.toSuccess(noCollectionIdInItem(item.id))
    oid <- if (ObjectId.isValid(id)) Success(new ObjectId(id)) else Failure(invalidCollectionId(id, item.id))
    accessibleItem <- if (organizationService.canAccessCollection(client.id, oid, Permission.Read))
      Success(item)
    else
      Failure(inaccessibleItem(item.id, client.id, Permission.Read))
  } yield accessibleItem

}
