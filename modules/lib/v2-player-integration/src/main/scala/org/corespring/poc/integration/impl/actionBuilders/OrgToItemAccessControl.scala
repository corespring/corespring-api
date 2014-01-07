package org.corespring.poc.integration.impl.actionBuilders

import org.bson.types.ObjectId
import org.corespring.platform.core.models.UserOrg
import org.corespring.platform.core.models.auth.Permission
import org.corespring.platform.core.services.UserService
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.core.services.organization.OrganizationService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.poc.integration.impl.securesocial.SecureSocialService
import play.api.mvc.{AnyContent, Request}
import scalaz.Scalaz._
import scalaz._
import securesocial.core.Identity

trait OrgToItemAccessControl {

  def orgService: OrganizationService

  def itemService: ItemService

  def secureSocialService: SecureSocialService

  def userService: UserService

  def orgCanAccessItem(permission: Permission, userOrg: UserOrg, itemId: VersionedId[ObjectId]): Validation[String, Boolean] = for {
    orgPermission <- Permission.fromLong(userOrg.pval).toSuccess("Can't parse pval")
    hasPermission <- if (orgPermission.has(permission)) Success(true) else Failure("Permission test failed")
    org <- orgService.findOneById(userOrg.orgId).toSuccess(s"Cant find org with id: ${userOrg.orgId}")
    item <- itemService.findOneById(itemId).toSuccess(s"Can't find item with id: ${itemId}")
    canAccessCollection <- if (orgService.canAccessCollection(org.id, new ObjectId(item.collectionId), permission)) Success(true) else Failure("Can't access collection")
  } yield true


  def userOrgFromRequest(request: Request[AnyContent]) : Option[UserOrg] = for {
    u <- secureSocialService.currentUser(request)
    o <- getUserOrg(u)
  } yield {
    o
  }

  private def getUserOrg(id: Identity): Option[UserOrg] = userService.getUser(id.identityId.userId, id.identityId.providerId).map(_.org)

}
