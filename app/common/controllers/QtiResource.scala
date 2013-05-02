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
  def getItemXMLByObjectId(itemId: String, callerOrg: ObjectId): Option[Elem] = {
    Item.findOneById(new ObjectId(itemId)) match {
      case Some(item) => {
        if (Content.isCollectionAuthorized(callerOrg, item.collectionId, Permission.Read)) {
          val dataResource = item.data.get
          dataResource.files.find(_.name == Resource.QtiXml).map{
            case VirtualFile(_,_,_,xml) => scala.xml.XML.loadString(xml)
          }
        } else None
      }
      case _ => None
    }
  }

}
