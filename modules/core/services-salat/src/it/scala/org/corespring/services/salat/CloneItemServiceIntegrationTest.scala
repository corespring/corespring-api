package org.corespring.services.salat

import org.corespring.errors.PlatformServiceError
import org.corespring.models.ContentCollection
import org.corespring.models.auth.Permission
import org.corespring.models.item.Item
import org.specs2.execute.Result
import org.specs2.specification.{Fragment, Scope}

import scala.concurrent.{ExecutionContext, Await}
import scala.concurrent.duration._
import scalaz.{Success, Validation}

class CloneItemServiceIntegrationTest extends ServicesSalatIntegrationTest {

  import ExecutionContext.Implicits.global

  trait scope extends Scope with InsertionHelper{

    val orgOne = insertOrg("org-one")
    val orgOneCollectionOne = services.orgCollectionService.getDefaultCollection(orgOne.id).toOption.get
    val itemOne = insertItem(orgOneCollectionOne.id)
    val service = services.cloneItemService
  }

  def assertClone(
    itemCollectionPerm: Permission,
    destinationCollectionPerm:Permission,
    fn: (Validation[PlatformServiceError,Item], ContentCollection) => Result) : Fragment = {
    assertClone("")(itemCollectionPerm, destinationCollectionPerm, fn)
  }

  def assertClone(msg:String)(
                   itemCollectionPerm: Permission,
                   destinationCollectionPerm:Permission,
                   fn: (Validation[PlatformServiceError,Item], ContentCollection) => Result) : Fragment = {
    val base =  s"clone from collection: ${itemCollectionPerm.name} -> ${destinationCollectionPerm.name}"
    val specLabel = if(msg.isEmpty) base else s"$base - $msg"

    specLabel in new scope{
      val otherOrg = insertOrg("other-org")
      //add the item collection permission
      val coll = insertCollection(s"other-org-coll-${itemCollectionPerm.name}", otherOrg)
      services.orgCollectionService.grantAccessToCollection(orgOne.id, coll.id, itemCollectionPerm)
      val destinationColl = insertCollection(s"coll-${destinationCollectionPerm.name}", otherOrg)
      services.orgCollectionService.grantAccessToCollection(orgOne.id, destinationColl.id, destinationCollectionPerm)
      val itemToClone = insertItem(coll.id)
      val cloneResult = service.cloneItem(itemToClone.id, orgOne.id, Some(destinationColl.id))
      val clonedItemResult = Await.result(cloneResult.map{id => services.itemService.findOneById(id).get}.future, 2.seconds)
      fn(clonedItemResult, destinationColl)
    }
  }

  "cloneItem" should {
    "clone an item to the same collection as the item" in new scope {
      val clonedItemId = Await.result(service.cloneItem(itemOne.id, orgOne.id, Some(orgOneCollectionOne.id)).future, 2.seconds).toOption.get
      val clonedItem = services.itemService.findOneById(clonedItemId).get
      clonedItem.collectionId must_== orgOneCollectionOne.id.toString
    }

    "clone an item to the same collection as the item if you don't specify a target" in new scope {
      val clonedItemId = Await.result(service.cloneItem(itemOne.id, orgOne.id, None).future, 2.seconds).toOption.get
      val clonedItem = services.itemService.findOneById(clonedItemId).get
      clonedItem.collectionId must_== orgOneCollectionOne.id.toString
    }

    assertClone(Permission.Clone, Permission.Write, (cloneResult, _) =>{
      cloneResult.isSuccess must_== true
    })

    assertClone("has the destination collection id")(Permission.Clone, Permission.Write, (cloneResult, d) =>{
      cloneResult.map(_.collectionId) must_== Success(d.id.toString)
    })

    assertClone("fails")(Permission.Clone, Permission.Clone, (cloneResult, _) =>{
      cloneResult.isFailure must_== true
    })

    assertClone("fails")(Permission.Clone, Permission.Read, (cloneResult, _) =>{
      cloneResult.isFailure must_== true
    })

    assertClone(Permission.Write, Permission.Write, (cloneResult, _) =>{
      cloneResult.isSuccess must_== true
    })

    assertClone("fails")(Permission.Write, Permission.Clone, (cloneResult, _) =>{
      cloneResult.isFailure must_== true
    })

    assertClone("fails")(Permission.Write, Permission.Read, (cloneResult, _) =>{
      cloneResult.isFailure must_== true
    })

    assertClone("fails")(Permission.Read, Permission.Write, (cloneResult, _) =>{
      cloneResult.isFailure must_== true
    })

    assertClone("fails")(Permission.Read, Permission.Clone, (cloneResult, _) =>{
      cloneResult.isFailure must_== true
    })

    assertClone("fails")(Permission.Read, Permission.Read, (cloneResult, _) =>{
      cloneResult.isFailure must_== true
    })

  }
}
