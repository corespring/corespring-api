/**
  *
  * We have a library of Services for the core business logic.
  * We have ways of authenticating requests and checking permissions on the web tier.
  *
  * However it would be an improvement to create a module build on the core services that has controls
  * access and execution of service calls. We could simplify the model that is used for making the request.
  * We could have a more declarative approach to defining access for methods in the services.
  */

import org.bson.types.ObjectId
import org.corespring.models.auth.Permission
import org.corespring.models.item.Item
import org.corespring.services.CollectionIdPermission
import org.corespring.services.item.ItemService

import scalaz.{Failure, Validation}

object Role extends Enumeration{
  type Role = Value
  val Org = Value
}

trait Restriction{
  def itemPermissions : Seq[ItemPermission]
  def collectionPermissions: Seq[CollectionIdPermission]
}

case class SimpleRestriction(itemPermissions : Seq[ItemPermission], collectionPermissions: Seq[CollectionIdPermission])

case class ItemPermission(item: Item, p: Permission)

trait Access{
  def restrictTo[A,R <: Restriction](r:R)(block: Boolean => A) = block(true)
}

/**
  *
  * Usage:
  * val access = Access(org) //build the access implementation based on this org.
  * val subjectService = new SubjectItemService(access, itemService)
  * subjectService.cloneToCollection(item, targetCollectionId)
  * @param access
  * @param underlying
  */
class SubjectItemService(
  access: Access,
  underlying : ItemService) extends ItemService{

  /**
    * Note: it would be better to just have clone, but that method is used in the [[org.corespring.services.item.BaseContentService]],
    * so hopefully we can remove that and the conflate the methods
    *
    * @param item
    * @param targetCollectionId - clone the item to this collection if specified else use the same collection as the item
    * @return
    */
  override def cloneToCollection(item: Item, targetCollectionId: ObjectId): Validation[String, Item] = access.restrictTo(
    SimpleRestriction(Seq(ItemPermission(item, Permission.Read)),
    Seq(CollectionIdPermission(targetCollectionId, Permission.Write)))){ _ =>
    underlying.cloneToCollection(item, targetCollectionId)
  }
}


