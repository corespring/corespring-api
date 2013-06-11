package common.controllers

import org.bson.types.ObjectId
import scala.xml.Elem
import models.item.{Item, Content}
import controllers.auth.Permission
import models.item.resource.{VirtualFile, Resource}
import models.item.service.ItemServiceClient

trait QtiResource { self : ItemServiceClient =>

  /**
   * Provides the item XML body for an item with a provided item id.
   * @param itemId
   * @return
   */
  def getItemXMLByObjectId(itemId: String, version : Option[Int] = None, orgId: ObjectId): Option[Elem] = if(Content.isAuthorized(orgId, new ObjectId(itemId), Permission.Read)){
    itemService.getQtiXml(new ObjectId(itemId), version)
  } else {
   None
  }

  @deprecated("use Item.getQtiXml instead", "versioning-dao")
  def getItemXMLByObjectId(item: Item, orgId: ObjectId): Option[Elem] = if(authorized(item,orgId)){
    item.data.map {
      d =>
        d.files.find(_.name == Resource.QtiXml).map {
          case VirtualFile(_, _, _, xml) => scala.xml.XML.loadString(xml)
        }.getOrElse( throw new RuntimeException("The qti xml file has a bad format and cannot be retrieved: itemId: " + item.id))
    }
  } else None

  private def authorized(i: Item, org: ObjectId): Boolean = Content.isCollectionAuthorized(org, i.collectionId, Permission.Read)

}
