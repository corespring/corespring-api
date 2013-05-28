package common.controllers

import org.bson.types.ObjectId
import scala.xml.Elem
import models.item.{Item, Content}
import controllers.auth.Permission
import models.item.resource.{VirtualFile, Resource}

trait QtiResource {

  /**
   * Provides the item XML body for an item with a provided item id.
   * @param itemId
   * @return
   */
  def getItemXMLByObjectId(itemId: String, orgId: ObjectId): Option[Elem] =
    Item.findOneById(new ObjectId(itemId)).map {
      getItemXMLByObjectId(_, orgId)
    }.getOrElse(None)


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
