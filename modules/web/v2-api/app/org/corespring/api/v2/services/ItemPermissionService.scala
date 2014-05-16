package org.corespring.api.v2.services

import org.corespring.platform.core.models.Organization
import org.corespring.platform.core.models.auth.Permission
import org.corespring.platform.core.models.item.Item
import scalaz.Validation

class ItemPermissionService extends PermissionService[Organization, Item] {
  override def create(client: Organization, newValue: Item): PermissionResult = {

    import scalaz.Scalaz._
    import scalaz.{ Failure, Success }

    val result: Validation[String, PermissionResult] = for {
      id <- newValue.collectionId.toSuccess(s"No collection id specified in item: ${newValue.id}")
      contentCollection <- client.contentcolls.find(_.collectionId.toString == id).toSuccess(s"$id is not accessible to Organization: ${client.id}")
      collPermission <- Permission.fromLong(contentCollection.pval).toSuccess(s"Can't parse permission for collection: ${contentCollection.collectionId}")
    } yield {
      if (collPermission.has(Permission.Write)) {
        Granted
      } else {
        Denied(s"${Permission.toHumanReadable(contentCollection.pval)} does not allow ${Permission.Write.name}")
      }
    }

    result match {
      case Failure(msg) => Denied(msg)
      case Success(permissionResult) => permissionResult
    }
  }
}
