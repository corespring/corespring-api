package common.controllers

import org.bson.types.ObjectId
import scala.xml.Elem
import org.corespring.platform.core.models.item.resource.{ VirtualFile, Resource }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.platform.core.models.item.{ Item, Content }
import org.corespring.platform.core.models.auth.Permission
import org.corespring.platform.core.services.item.ItemServiceClient

trait QtiResource { self: ItemServiceClient =>

  /**
   * Provides the item XML body for an item with a provided item id.
   * @param itemId
   * @return
   */
  def getItemXMLByObjectId(itemId: VersionedId[ObjectId], orgId: ObjectId): Option[Elem] = if (Content.isAuthorized(orgId, itemId, Permission.Read)) {
    itemService.getQtiXml(itemId)
  } else {
    None
  }

  @deprecated("use Item.getQtiXml instead", "versioning-dao")
  def getItemXMLByObjectId(item: Item, orgId: ObjectId): Option[Elem] = if (authorized(item, orgId)) {
    item.data.map {
      d =>
        d.files.find(_.name == Item.QtiResource.QtiXml).map {
          case VirtualFile(_, _, _, xml) => scala.xml.XML.loadString(xml)
        }.getOrElse(throw new RuntimeException("The qti xml file has a bad format and cannot be retrieved: itemId: " + item.id))
    }
  } else None

  private def authorized(i: Item, org: ObjectId): Boolean = Content.isCollectionAuthorized(org, i.collectionId, Permission.Read)

}
