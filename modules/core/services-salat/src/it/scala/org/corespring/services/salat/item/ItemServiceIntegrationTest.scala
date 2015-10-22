package org.corespring.services.salat.item

import org.bson.types.ObjectId
import org.corespring.models.ContentCollection
import org.corespring.models.auth.Permission
import org.corespring.models.item._
import org.corespring.models.item.resource.{ Resource, StoredFile }
import org.corespring.platform.data.mongo.exceptions.SalatVersioningDaoException
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.salat.ServicesSalatIntegrationTest
import org.joda.time.DateTime
import org.specs2.mutable.{ After, BeforeAfter }

import scalaz.{ Failure, Success }

class ItemServiceIntegrationTest extends ServicesSalatIntegrationTest {

  val service = services.itemService

  trait scope extends BeforeAfter with InsertionHelper {
    lazy val org = insertOrg("1")
    val collectionOne = insertCollection("collection-one", org)
    val itemOne = addItem(1, collectionOne)

    def before: Any = {}

    def after: Any = removeAllData()

    def addItem(id: Int, c: ContentCollection,
      contributorId: Option[Int] = None,
      contentType: Option[String] = None,
      standards: Seq[String] = Seq.empty,
      title: Option[String] = None) = {
      val contributorDetails = ContributorDetails(
        contributor = Some("contributor-" + contributorId.getOrElse(id)))
      val item = Item(
        collectionId = c.id.toString,
        contributorDetails = Some(contributorDetails),
        contentType = contentType.getOrElse(Item.contentType),
        standards = standards,
        taskInfo = Some(TaskInfo(title = title)))
      services.itemService.insert(item)
      item
    }

    def loadItem(id: VersionedId[ObjectId]): Option[Item] = service.findOneById(id)
  }

  "countItemsInCollection" should {

    "return 1 for collection with 1 item" in new scope {
      service.countItemsInCollection(collectionOne.id) must_== 1
    }

    "return 0 for collection with no items" in new scope {
      val collectionTwo = insertCollection("two", org)
      service.countItemsInCollection(collectionTwo.id) must_== 0
    }
  }

  "addFileToPlayerDefinition" should {
    trait addFileToPlayerDefinition extends scope

    "add file to playerDefinition.files using item id" in new addFileToPlayerDefinition {
      val file = StoredFile("name.png", "image/png", false)
      service.addFileToPlayerDefinition(itemOne.id, file)
      loadItem(itemOne.id).map(_.playerDefinition.get.files === Seq(file))
    }

    "add file to playerDefinition.files using item" in new addFileToPlayerDefinition {
      val file = StoredFile("name.png", "image/png", false)
      service.addFileToPlayerDefinition(itemOne, file)
      loadItem(itemOne.id).map(_.playerDefinition.get.files === Seq(file))
    }
    "return true when call was successful" in new addFileToPlayerDefinition {
      val file = StoredFile("name.png", "image/png", false)
      service.addFileToPlayerDefinition(itemOne, file) match {
        case Success(res) => res === true
        case Failure(e) => failure(s"Unexpected error $e")
      }
    }
    //TODO Do we want it to throw?
    "throw error when item cannot be found" in new addFileToPlayerDefinition {
      val file = StoredFile("name.png", "image/png", false)
      service.addFileToPlayerDefinition(VersionedId(ObjectId.get), file) must throwA[SalatVersioningDaoException]
    }
  }

  "asMetadataOnly" should {
    trait asMetadataOnly extends scope {
      val longAgo = new DateTime(1000, 10, 10, 10, 10)

      val item = new Item(
        id = VersionedId(ObjectId.get),
        supportingMaterials = Seq(Resource(name = "test", files = Seq.empty)),
        data = Some(Resource(name = "test-data", files = Seq.empty)),
        collectionId = "1234567",
        dateModified = Some(longAgo))
    }
    "set dateModified to the current time" in new asMetadataOnly {
      val now = DateTime.now().getMillis
      var dateModified = service.asMetadataOnly(item).get("dateModified")
      new DateTime(dateModified).getMillis must beGreaterThanOrEqualTo(now)
    }
    "remove id from result" in new asMetadataOnly {
      val res = service.asMetadataOnly(item)
      res.containsField("id") === false
    }
    "remove supportingMaterials from result" in new asMetadataOnly {
      val res = service.asMetadataOnly(item)
      res.containsField("supportingMaterials") === false
    }
    "remove data from result" in new asMetadataOnly {
      val res = service.asMetadataOnly(item)
      res.containsField("data") === false
    }
    "remove collectionId from result" in new asMetadataOnly {
      val res = service.asMetadataOnly(item)
      res.containsField("collectionId") === false
    }
  }

  "clone" should {
    trait clone extends scope {
      val item = Item(collectionId = "1234567")
      val clonedItem = service.clone(item)
    }
    "return the cloned item" in new clone {
      clonedItem.get.id !== item.id
    }
    "create a new item in the db" in new clone {
      loadItem(clonedItem.get.id).isDefined == true
    }
    //TODO How much of file cloning do we want to test?
    "clone stored files" in pending
  }

  "collectionIdForItem" should {
    trait collectionIdForItem extends scope {
      val itemId = ObjectId.get
      val v0CollectionId = ObjectId.get
      val v0Item = Item(id = VersionedId(itemId, Some(0)), collectionId = v0CollectionId.toString)
      val v1CollectionId = ObjectId.get
      val v1Item = Item(id = VersionedId(itemId, Some(1)), collectionId = v1CollectionId.toString)

      override def before = service.insert(v1Item)
      override def after = removeAllData()
    }

    "return the collectionId of the item" in new collectionIdForItem {
      val res = service.collectionIdForItem(v1Item.id) === Some(v1CollectionId)
    }

    "always return the collectionId of the last version of the item" in new collectionIdForItem {
      val res = service.collectionIdForItem(v0Item.id) === Some(v1CollectionId)
    }

    "return None if item does not exist" in new collectionIdForItem {
      val res = service.collectionIdForItem(VersionedId(ObjectId.get)) === None
    }

    "return None if collectionId is not an ObjectId" in new collectionIdForItem {
      val itemWithInvalidCollectionId = Item(collectionId = "this is not an ObjectId")
      service.insert(itemWithInvalidCollectionId) !== None
      val res = service.collectionIdForItem(itemWithInvalidCollectionId.id) must_== None
    }
  }

  "contributorsForOrg" should {

    "return single contributor from one collection for an org" in new scope {
      val res = service.contributorsForOrg(org.id)
      res === Seq("contributor-1")
    }

    "return multiple contributors from one collection for an org" in new scope {
      addItem(2, collectionOne, contributorId = Some(2))
      service.contributorsForOrg(org.id) === Seq("contributor-1", "contributor-2")
    }

    "return multiple contributors from multiple collection for an org" in new scope {
      val collectionTwo = insertCollection("two", org)
      addItem(2, collectionTwo, Some(2))
      service.contributorsForOrg(org.id) === Seq("contributor-1", "contributor-2")
    }

    "not include a contributor more than once" in new scope {
      val collectionTwo = insertCollection("two", org)
      addItem(2, collectionTwo, Some(1))
      addItem(3, collectionTwo, Some(77))
      addItem(4, collectionTwo, Some(77))
      service.contributorsForOrg(org.id) === Seq("contributor-1", "contributor-77")
    }
    "include contributors from collections that the org has write access to" in new scope {
      val otherOrg = insertOrg("other-org")
      val otherOrgCollection = insertCollection("other-org-collection", otherOrg)
      addItem(2, otherOrgCollection, contributorId = Some(333))
      giveOrgAccess(org, otherOrgCollection, Permission.Write)
      service.contributorsForOrg(org.id) === Seq("contributor-1", "contributor-333")
    }

    "include contributors from collections that the org has read access to" in new scope {
      val otherOrg = insertOrg("other-org")
      val otherOrgCollection = insertCollection("other-org-collection", otherOrg)
      addItem(2, otherOrgCollection, contributorId = Some(333))
      giveOrgAccess(org, otherOrgCollection, Permission.Read)
      service.contributorsForOrg(org.id) === Seq("contributor-1", "contributor-333")
    }

    "not include contributors from collections that the org has no access to" in new scope {
      val otherOrg = insertOrg("other-org")
      val otherOrgCollection = insertCollection("other-org-collection", otherOrg)
      addItem(2, otherOrgCollection, contributorId = Some(333))
      service.contributorsForOrg(org.id) === Seq("contributor-1")
    }

    "not include contributors from archived items" in new scope {
      services.itemService.moveItemToArchive(itemOne.id)
      service.contributorsForOrg(org.id) === Seq.empty
    }

    "not include contributors from items in versionedContent" in new scope {
      val updatedItem = itemOne.copy(
        contributorDetails = Some(ContributorDetails(contributor = Some("updated contributor"))))
      service.save(updatedItem, createNewVersion = true)

      //the versioned item has the old contributor still
      service.findOneById(itemOne.id).map(_.contributorDetails.get.contributor === Some("contributor-1"))
      service.contributorsForOrg(org.id) === Seq("updated contributor")
    }
  }

  "currentVersion" should {
    "return 0 as the the current version of a new item" in new scope {
      service.currentVersion(itemOne.id) === 0
    }
    "return 1 as the the current version of an updated item" in new scope {
      service.save(itemOne, true)
      service.currentVersion(itemOne.id) === 1
    }
    //TODO Shouldn't that result in an error?
    "return 0 for a non existing item" in new scope {
      service.currentVersion(VersionedId(ObjectId.get)) === 0
    }
  }

  "find" should {
    "only return item of type item" in pending
    "not return archived items" in pending
    "allow to select the returned fields" in pending
    "return an empty Stream if no items can be found" in pending
  }

  "findFieldsById" should {
    "return all fields of the item by default" in pending
    "allow to select fields of the item" in pending
    "return None if item cannot be found" in pending
    "return Fields of archived item" in pending
  }

  "findItemStandards" should {

    "return item standards of an item" in new scope {
      override val itemOne = addItem(1, collectionOne, title = Some("title"), standards = Seq("S1", "S2"))
      service.findItemStandards(itemOne.id) must_== Some(ItemStandards("title", Seq("S1", "S2"), itemOne.id))
    }
    "return item standards of an archived item" in pending
    "return None if item cannot be found" in pending
    "return None if item has no title" in pending
    "return None if item has no standards" in pending

  }

  "findMultiple" should {
    "return Seq of items found" in pending
    "return empty Seq when o item can be found" in pending
    "return items with type item only" in pending
    "allow to select fields of the items" in pending
    "not return archived items" in pending
  }

  "findMultipleById" should {
    "return Stream of items found" in pending
    "return empty Stream if no item can be found" in pending
    "not return archived items" in pending
  }

  "findOne" should {
    "return item" in pending
    "return None if item is in archive" in pending
    "return None if item cannot be found" in pending
  }

  "findOneById" should {
    "return current item" in pending
    "return archived item" in pending
    "return None if item cannot be found" in pending
  }

  "getOrCreateUnpublishedVersion" should {
    "return an existing unpublished current item" in pending
    "return None if the item does not exist in current or archive" in pending
    "create a new unpublished item, if published item can be found in current" in pending
    "create a new unpublished item, if published item can be found in archive" in pending
    "create a new unpublished item, if unpublished item can be found in archive" in pending
  }

  "getQtiXml" should {
    //TODO Really? Or is it a todo?
    "return None for any item" in pending
  }

  "insert" should {
    "return the id if successful" in pending
    "return None if not successful" in pending
  }

  "isPublished" should {
    "return true if item.isPublished is true" in pending
    "return false if item.isPublished is false" in pending
    "return false if item cannot be found" in pending
    "return false if item is archived" in pending
  }

  "moveItemToArchive" should {
    "set the collectionId of an item to the archive collection id" in pending
    "not add a new item, if it does not exist" in pending
    "return the archive collection id" in pending
  }

  "publish" should {
    "set item.isPublished to true" in pending
    "not create a new item, if it does not exist" in pending
    "return true, if update is successful" in pending
    "return true, if isPublished was true already" in pending
    "return false, if item could not be updated" in pending
  }

  "purge" should {
    "delete item from current" in pending
    "delete item from archive" in pending
    "return Success when item has been deleted" in pending
    "return Success when item has not been deleted" in pending
  }

  "removeCollectionIdsFromShared" should {
    "remove one collectionId from one item" in pending
    "remove multiple collectionIds from one item" in pending
    "remove one collectionId from multiple items" in pending
    "remove multiple collectionId from multiple items" in pending
    "return ids of all items if successful" in pending
    "return ids of failed items if not successful" in pending
  }

  "saveNewUnpublishedVersion" should {
    "create new unpublished item when item is in current" in pending
    "create new unpublished item when item is in archive" in pending
    "return None if the item cannot be found in current or archive" in pending
  }

  "saveUsingDbo" should {
    "return false when item does not exist in current" in pending
    "return true when item exists in current" in pending
    "use the dbo to update the item" in pending
    "create a new version of the item, when createNewVersion is true" in pending

    //TODO Assets should be copied, that seems to be missing in the implementation
    "copy the assets when createNewVersion is true" in pending
  }

}
