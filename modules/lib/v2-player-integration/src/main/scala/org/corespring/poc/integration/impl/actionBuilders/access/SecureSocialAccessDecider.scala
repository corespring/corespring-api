package org.corespring.poc.integration.impl.actionBuilders.access

import org.bson.types.ObjectId
import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.core.models.UserOrg
import org.corespring.platform.core.models.auth.Permission
import org.corespring.platform.core.services.UserService
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.core.services.organization.OrganizationService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.poc.integration.impl.actionBuilders.OrgToItemAccessControl
import org.corespring.poc.integration.impl.securesocial.SecureSocialService
import play.api.libs.json.JsValue
import play.api.mvc.{AnyContent, Request}
import scalaz.Scalaz._
import scalaz._
import securesocial.core.Identity

class SecureSocialAccessDecider(
                                 val secureSocialService: SecureSocialService,
                                 val userService: UserService,
                                 val sessionService: MongoService,
                                 val itemService: ItemService,
                                 val orgService: OrganizationService
                                 ) extends AccessDecider with OrgToItemAccessControl {

  private def getUserOrg(id: Identity): Option[UserOrg] = userService.getUser(id.identityId.userId, id.identityId.providerId).map(_.org)

  private def getItemId(session: JsValue): Option[VersionedId[ObjectId]] = {
    (session \ "itemId").asOpt[String].map(VersionedId(_)).flatten
  }

  def accessForItemId(itemId: String, request: Request[AnyContent]): AccessResult = {
    accessWithItemId {
      _ => VersionedId(itemId)
    }(request)
  }

  def accessForSessionId(sessionId: String, request: Request[AnyContent]): AccessResult = {
    accessWithItemId {
      _ =>
        sessionService.load(sessionId).flatMap(getItemId(_))
    }(request)
  }

  private def accessWithItemId(getItemId: Unit => Option[VersionedId[ObjectId]])(request: Request[AnyContent]): AccessResult = {
    val validationResult: Validation[String, Boolean] = for {
      u <- secureSocialService.currentUser(request).toSuccess("No user")
      o <- getUserOrg(u).toSuccess(s"No user org found for: ${u.identityId.userId}")
      itemId <- getItemId().toSuccess("Can't find or parse item Id")
      canAccess <- orgCanAccessItem(Permission.Read, o, itemId)
    } yield {
      canAccess
    }

    validationResult match {
      case Success(true) => AccessResult(true, Seq.empty)
      case Success(false) => AccessResult(false, Seq("Access denied"))
      case Failure(e) => AccessResult(false, Seq(e))
    }
  }

}
