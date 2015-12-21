package org.corespring.v2.auth

import org.bson.types.ObjectId
import org.corespring.models.auth.Permission
import org.corespring.models.item.{Item, Passage}
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.OrgCollectionService
import org.corespring.services.item.ItemService
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.Errors.orgCantAccessCollection
import org.corespring.v2.errors.V2Error
import play.api.libs.json._

import scalaz.{Success, Failure, Validation}

class PassageAccess(orgCollectionService: OrgCollectionService, itemService: ItemService)
  extends Access[(Passage, Option[VersionedId[ObjectId]]), OrgAndOpts] {

  private def containsPassageWithId(item: Item, passageId: String): Boolean = item.playerDefinition.map(pd => pd.components match {
    case obj: JsObject =>
      obj.keys
        .map(key => (obj \ key).as[JsObject])
        .find(component =>
        (component \ "componentType").asOpt[String] == Some("corespring-passage") &&
          (component \ "id").asOpt[String] == Some(passageId)).nonEmpty
    case _ => false
  }).getOrElse(false)

  override def grant(identity: OrgAndOpts, permission: Permission,
                     passageAndItemId: (Passage, Option[VersionedId[ObjectId]])): Validation[V2Error, Boolean] = {

    def orgCanAccess(collectionId: String) = orgCollectionService.isAuthorized(identity.org.id, new ObjectId(collectionId), permission)

    /**
     * Returns true if the item id in question can access (i.e., has a reference to) the provided passage. Also the
     * identity in scope must be able to access the item in question.
     */
    def itemCanAccess(itemId: Option[VersionedId[ObjectId]], passage: Passage) = itemId match {
      case Some(itemId) => itemService.findOneById(itemId) match {
        case Some(item) => orgCanAccess(item.collectionId) && containsPassageWithId(item, passage.id.toString)
        case _ => false
      }
      case _ => false
    }

    val (passage, itemId) = passageAndItemId

    orgCanAccess(passage.collectionId) match {
      case true => Success(true)
      case false => itemCanAccess(itemId, passage) match {
        case true => Success(true)
        case _ => Failure(orgCantAccessCollection(identity.org.id, passage.collectionId, permission.name))
      }
    }
  }

}