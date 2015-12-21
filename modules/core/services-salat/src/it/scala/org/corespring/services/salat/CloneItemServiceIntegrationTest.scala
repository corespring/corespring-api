package org.corespring.services.salat

import org.corespring.models.auth.Permission
import org.specs2.execute.Result
import org.specs2.specification.{ Fragment, Scope }

import scala.concurrent.{ ExecutionContext, Await }
import scala.concurrent.duration._
import scalaz.{ Failure, Success }

class CloneItemServiceIntegrationTest extends ServicesSalatIntegrationTest {

  import ExecutionContext.Implicits.global

  trait scope extends Scope with InsertionHelper {

    val orgOne = insertOrg("org-one")
    val orgOneCollectionOne = services.orgCollectionService.getDefaultCollection(orgOne.id).toOption.get
    val itemOne = insertItem(orgOneCollectionOne.id)
    val service = services.cloneItemService
  }

  trait cloneScope extends scope {
    def itemCollectionPerm: Permission
    def destinationCollectionPerm: Permission
    val otherOrg = insertOrg("other-org")
    //add the item collection permission
    val itemCollection = insertCollection(s"other-org-coll-${itemCollectionPerm.name}", otherOrg)
    services.orgCollectionService.grantAccessToCollection(orgOne.id, itemCollection.id, itemCollectionPerm)
    val destinationColl = insertCollection(s"coll-${destinationCollectionPerm.name}", otherOrg)
    services.orgCollectionService.grantAccessToCollection(orgOne.id, destinationColl.id, destinationCollectionPerm)
    val itemToClone = insertItem(itemCollection.id)
    val cloneFutureV = service.cloneItem(itemToClone.id, orgOne.id, Some(destinationColl.id))
    val cloneResult = Await.result(cloneFutureV.future, 2.seconds)
    val clonedItemResult = cloneResult.map { id => services.itemService.findOneById(id).get }
  }

  def assertClone(
    itemCollectionPerm: Permission,
    destinationCollectionPerm: Permission)(fn: (cloneScope) => Result): Fragment = {
    assertClone("")(itemCollectionPerm, destinationCollectionPerm)(fn)
  }

  def assertClone(msg: String)(icp: Permission, dcp: Permission)(fn: (cloneScope) => Result): Fragment = {
    val base = s"clone from item collection ${icp.name} --> destination collection ${dcp.name}"
    val specLabel = if (msg.isEmpty) base else s"$base - $msg"

    specLabel in new cloneScope {
      override def itemCollectionPerm: Permission = icp
      override def destinationCollectionPerm: Permission = dcp
      fn(this)
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

    import Permission._

    assertClone(Clone, Write)((s: cloneScope) => {
      s.cloneResult.isSuccess must_== true
    })

    import org.corespring.errors.collection.{ CantWriteToCollection, CantCloneFromCollection }

    assertClone("has the destination collection id")(Clone, Write)(s => {
      s.clonedItemResult.map(_.collectionId) must_== Success(s.destinationColl.id.toString)
    })

    assertClone(Clone, Write)(s => {
      s.cloneResult.isSuccess must_== true
    })

    assertClone("fails")(Clone, Clone)(s => {
      s.cloneResult must_== Failure(CantWriteToCollection(s.orgOne.id, Some(Clone), s.destinationColl.id))
    })

    assertClone("fails")(Clone, Read)(s => {
      s.cloneResult must_== Failure(CantWriteToCollection(s.orgOne.id, Some(Read), s.destinationColl.id))
    })

    assertClone(Write, Write)(s => {
      s.cloneResult.isSuccess must_== true
    })

    assertClone("fails")(Write, Clone)(s => {
      s.cloneResult must_== Failure(CantWriteToCollection(s.orgOne.id, Some(Clone), s.destinationColl.id))
    })

    assertClone("fails")(Write, Read)(s => {
      s.cloneResult must_== Failure(CantWriteToCollection(s.orgOne.id, Some(Read), s.destinationColl.id))
    })

    assertClone("fails")(Read, Write)(s => {
      s.cloneResult must_== Failure(CantCloneFromCollection(s.orgOne.id, Some(Read), s.itemCollection.id))
    })

    assertClone("fails")(Read, Clone)(s => {
      s.cloneResult must_== Failure(CantWriteToCollection(s.orgOne.id, Some(Clone), s.destinationColl.id))
    })

    assertClone("fails")(Read, Read)(s => {
      s.cloneResult must_== Failure(CantWriteToCollection(s.orgOne.id, Some(Read), s.destinationColl.id))
    })

  }
}
