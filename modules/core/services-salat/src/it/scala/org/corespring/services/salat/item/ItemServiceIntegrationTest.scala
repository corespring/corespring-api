package org.corespring.services.salat.item

import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId
import org.corespring.errors.PlatformServiceError
import org.corespring.models.ContentCollection
import org.corespring.models.auth.Permission
import org.corespring.models.item._
import org.corespring.models.item.resource.{ Resource, StoredFile }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.item.ItemCount
import org.corespring.services.salat.ServicesSalatIntegrationTest
import org.joda.time.DateTime
import org.specs2.mutable.{ After, BeforeAfter }

import scalaz.{ Failure, Success }

/**
 * Note: Items can be stored in three different collections
 * 1. content - contains the current version of items
 * 2. versioned_content - contains old versions of items. That happens automatically
 * when you save an item with createNewVersion=true
 * 3. archive collection - contains items that have been archived using moveToArchive
 *
 * The dao methods with "current" in the name only use the content collection, while the
 * other methods use both, the content and versioned_content collection
 */
class ItemServiceIntegrationTest extends ServicesSalatIntegrationTest {

  val service = services.itemService

  trait scope extends BeforeAfter with InsertionHelper {
    val org = insertOrg("1")
    val collectionOne = insertCollection("collection-one", org)
    val itemOne = addItem(1, collectionOne)

    def randomItemId = VersionedId(ObjectId.get)

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
        playerDefinition = Some(PlayerDefinition.empty),
        taskInfo = Some(TaskInfo(title = title)))

      services.itemService.insert(item)
      item
    }

    def loadItem(id: VersionedId[ObjectId]): Option[Item] = service.findOneById(id)

    def idQuery(id: VersionedId[ObjectId]) = MongoDBObject("_id._id" -> id.id, "_id.version" -> id.version)
  }

  "addFileToPlayerDefinition" should {
    trait addFileToPlayerDefinition extends scope

    "add file to playerDefinition.files using item id" in new addFileToPlayerDefinition {
      val file = StoredFile("name.png", "image/png", false)
      service.addFileToPlayerDefinition(itemOne.id, file)
      loadItem(itemOne.id).map(_.playerDefinition.get.files) must_== Some(Seq(file))
    }

    "add file to playerDefinition.files using item" in new addFileToPlayerDefinition {
      val file = StoredFile("name.png", "image/png", false)
      service.addFileToPlayerDefinition(itemOne, file)
      loadItem(itemOne.id).map(_.playerDefinition.get.files) must_== Some(Seq(file))
    }
    "return true when call was successful" in new addFileToPlayerDefinition {
      val file = StoredFile("name.png", "image/png", false)
      service.addFileToPlayerDefinition(itemOne, file) match {
        case Success(res) => res must_== true
        case Failure(e) => failure(s"Unexpected error $e")
      }
    }
    //TODO Do we want it to throw?
    "throw error when item cannot be found" in new addFileToPlayerDefinition {
      val file = StoredFile("name.png", "image/png", false)
      service.addFileToPlayerDefinition(randomItemId, file) must throwA[Throwable]
    }
  }

  "clone" should {
    trait clone extends scope {
      val collectionId = ObjectId.get.toString
      val item = Item(collectionId = collectionId)
      val clonedItem = service.clone(item)
    }

    "return the cloned item" in new clone {
      clonedItem.toOption.isDefined must_== true
      clonedItem.map(_.id) must_!= Success(item.id)
    }

    "create a new item in the db" in new clone {
      loadItem(clonedItem.toOption.get.id).isDefined must_== true
    }
    //TODO How much of file cloning do we want to test?
    "clone stored files" in pending
  }

  "cloneToCollection" should {

    trait cloneToCollection extends scope {
      val otherOrg = insertOrg("other-org")
      val otherOrgCollection = services.orgCollectionService.getDefaultCollection(otherOrg.id).toOption.get
      val clonedItem = service.cloneToCollection(itemOne, otherOrgCollection.id)
    }

    "cloned item has the new collection id" in new cloneToCollection {
      clonedItem.toOption.get.collectionId must_== otherOrgCollection.id.toString
    }
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
      service.collectionIdForItem(v1Item.id) must_== Some(v1CollectionId)
    }

    "always return the collectionId of the last version of the item" in new collectionIdForItem {
      service.collectionIdForItem(v0Item.id) must_== Some(v1CollectionId)
    }

    "return None if item does not exist" in new collectionIdForItem {
      service.collectionIdForItem(randomItemId) must_== None
    }

    "return None if collectionId is not an ObjectId" in new collectionIdForItem {
      val itemWithInvalidCollectionId = Item(collectionId = "this is not an ObjectId")
      service.insert(itemWithInvalidCollectionId) must_!= None
      val res = service.collectionIdForItem(itemWithInvalidCollectionId.id) must_== None
    }
  }

  "contributorsForOrg" should {

    "return single contributor from one collection for an org" in new scope {
      val res = service.contributorsForOrg(org.id)
      res must_== Seq("contributor-1")
    }

    "return multiple contributors from one collection for an org" in new scope {
      addItem(2, collectionOne, contributorId = Some(2))
      service.contributorsForOrg(org.id) must_== Seq("contributor-1", "contributor-2")
    }

    "return multiple contributors from multiple collection for an org" in new scope {
      val collectionTwo = insertCollection("two", org)
      addItem(2, collectionTwo, Some(2))
      service.contributorsForOrg(org.id) must_== Seq("contributor-1", "contributor-2")
    }

    "not include a contributor more than once" in new scope {
      val collectionTwo = insertCollection("two", org)
      addItem(2, collectionTwo, Some(1))
      addItem(3, collectionTwo, Some(77))
      addItem(4, collectionTwo, Some(77))
      service.contributorsForOrg(org.id) must_== Seq("contributor-1", "contributor-77")
    }
    "include contributors from collections that the org has write access to" in new scope {
      val otherOrg = insertOrg("other-org")
      val otherOrgCollection = insertCollection("other-org-collection", otherOrg)
      addItem(2, otherOrgCollection, contributorId = Some(333))
      giveOrgAccess(org, otherOrgCollection, Permission.Write)
      service.contributorsForOrg(org.id) must_== Seq("contributor-1", "contributor-333")
    }

    "include contributors from collections that the org has read access to" in new scope {
      val otherOrg = insertOrg("other-org")
      val otherOrgCollection = insertCollection("other-org-collection", otherOrg)
      addItem(2, otherOrgCollection, contributorId = Some(333))
      giveOrgAccess(org, otherOrgCollection, Permission.Read)
      service.contributorsForOrg(org.id) must_== Seq("contributor-1", "contributor-333")
    }

    "not include contributors from collections that the org has no access to" in new scope {
      val otherOrg = insertOrg("other-org")
      val otherOrgCollection = insertCollection("other-org-collection", otherOrg)
      addItem(2, otherOrgCollection, contributorId = Some(333))
      service.contributorsForOrg(org.id) must_== Seq("contributor-1")
    }

    "not include contributors from archived items" in new scope {
      services.itemService.moveItemToArchive(itemOne.id)
      service.contributorsForOrg(org.id) must_== Seq.empty
    }

    "not include contributors from items in versionedContent" in new scope {
      val updatedItem = itemOne.copy(
        contributorDetails = Some(ContributorDetails(contributor = Some("updated contributor"))))
      service.save(updatedItem, createNewVersion = true)

      //check that the versioned item still has the old contributor
      service.findOneById(itemOne.id).map(_.contributorDetails.get.contributor must_== Some("contributor-1"))
      service.contributorsForOrg(org.id) must_== Seq("updated contributor")
    }

    "not include contributors from items with a contentType != item" in new scope {
      val unusualItem = itemOne.copy(
        id = randomItemId,
        contributorDetails = Some(ContributorDetails(contributor = Some("contributor from an unusual item"))),
        contentType = "not the usual content type")
      service.insert(unusualItem)
      service.contributorsForOrg(org.id) must_== Seq("contributor-1")
    }
  }

  "countItemsInCollections" should {
    trait countItemsInCollections extends scope

    "return 1 ItemCount with a count of 1" in new countItemsInCollections {
      service.countItemsInCollections(collectionOne.id) must equalTo(Seq(ItemCount(collectionOne.id, 1))).await
    }

    "return 1 ItemCount with a count of 20" in new countItemsInCollections {

      (1 to 19).foreach { i =>
        insertItem(collectionOne.id)
      }

      service.countItemsInCollections(collectionOne.id) must equalTo(Seq(ItemCount(collectionOne.id, 20))).await
    }

    "return multiple ItemCounts with a count of 0" in new countItemsInCollections {

      val collections = (1 to 19).map { i =>
        insertCollection(s"collection-$i", org)
      }

      val ids = collections.map(_.id)
      val expectedCounts = collections.map { c =>
        ItemCount(c.id, 0)
      }

      service.countItemsInCollections(ids: _*) must equalTo(expectedCounts).await
    }

    "return multiple ItemCounts with a counts of 0 and 1" in new countItemsInCollections {

      val collections = (0 to 3).map { i =>
        val coll = insertCollection(s"collection-$i", org)
        if (i < 2) {
          insertItem(coll.id)
        }
        coll
      }

      val ids = collections.map(_.id)
      val expectedCounts = collections.map { c =>
        val count = if (collections.indexOf(c) < 2) 1 else 0
        ItemCount(c.id, count)
      }

      service.countItemsInCollections(ids: _*) must equalTo(expectedCounts).await
    }
  }

  "currentVersion" should {
    "return 0 as the the current version of a new item" in new scope {
      service.currentVersion(itemOne.id) must_== 0
    }
    "return 1 as the the current version of an updated item" in new scope {
      service.save(itemOne, true)
      service.currentVersion(itemOne.id) must_== 1
    }
    //TODO Shouldn't that result in an error?
    "return 0 for a non existing item" in new scope {
      service.currentVersion(randomItemId) must_== 0
    }
  }

  "findFieldsById" should {
    //TODO findFieldsById api is deprecated, don't need to test now
    "allow to select fields of the item" in pending
    "return None if item cannot be found" in pending
    "return fields of old versions of item" in pending
  }

  "findItemStandards" should {

    "return item standards of an item" in new scope {
      val itemTwo = addItem(2, collectionOne, title = Some("title"), standards = Seq("S1", "S2"))
      service.findItemStandards(itemTwo.id) must_== Some(ItemStandards("title", Seq("S1", "S2"), itemTwo.id))
    }
    "return item standards of an old version of an item" in new scope {
      val itemTwo = addItem(2, collectionOne, title = Some("title"), standards = Seq("S1", "S2"))
      val oldVersion = itemTwo.id
      service.save(itemTwo, createNewVersion = true)
      service.findItemStandards(oldVersion) must_== Some(ItemStandards("title", Seq("S1", "S2"), oldVersion))
    }
    "return None if item cannot be found" in new scope {
      service.findItemStandards(randomItemId) must_== None
    }
    "return None if item has no title" in new scope {
      val itemTwo = addItem(2, collectionOne, title = None, standards = Seq("S1", "S2"))
      service.findItemStandards(itemTwo.id) must_== None
    }
  }

  "findMultipleById" should {
    "return item " in new scope {
      service.findMultipleById(itemOne.id.id).head.id must_== itemOne.id
    }
    "return Stream of items found" in new scope {
      val itemTwo = addItem(2, collectionOne)
      service.findMultipleById(itemOne.id.id, itemTwo.id.id).toSeq.map(_.id) must_== Seq(itemOne.id, itemTwo.id)
    }
    "return empty Stream if no item can be found" in new scope {
      service.findMultipleById(randomItemId.id) must_== Stream.empty
    }
    "not return old versions of an item" in new scope {
      val oldVersion = itemOne.id
      service.save(itemOne, createNewVersion = true)
      service.findMultipleById(oldVersion.id).length must_== 1
    }
  }

  "findOneById" should {
    //TODO Same as dao
    "return current item" in pending
    "return old versions of an item" in pending
    "return None if item cannot be found" in pending
  }

  "getOrCreateUnpublishedVersion" should {
    "return an existing unpublished current item" in new scope {
      service.getOrCreateUnpublishedVersion(itemOne.id) must_== Some(itemOne)
    }
    "return None if the item does not exist in current or old versions" in new scope {
      service.getOrCreateUnpublishedVersion(randomItemId) must_== None
    }
    "create a new unpublished item, if published item can be found in current" in new scope {
      service.publish(itemOne.id) must_== true
      val res = service.getOrCreateUnpublishedVersion(itemOne.id)
      res.isDefined must_== true
      res must_!= Some(itemOne.id)
    }

    "create a new unpublished item, if published item can be found in old versions" in new scope {
      service.publish(itemOne.id) must_== true
      val oldId = itemOne.id
      service.save(itemOne, createNewVersion = true).toOption.get
      val res = service.getOrCreateUnpublishedVersion(oldId)
      res.isDefined must_== true
      res must_!= Some(oldId)
    }

    "create a new unpublished item, if unpublished item can be found in old versions" in new scope {
      val oldId = itemOne.id
      service.save(itemOne, createNewVersion = true).toOption.get
      val res = service.getOrCreateUnpublishedVersion(oldId)
      res.isDefined must_== true
      res must_!= Some(oldId)
    }
  }

  "insert" should {
    //TODO Same as dao
    "return the id if successful" in pending
    "return None if not successful" in pending
  }

  "moveItemToArchive" should {

    def archiveCollectionId = services.archiveConfig.contentCollectionId.toString

    "set the collectionId of an item to the archive collection id" in new scope {
      service.moveItemToArchive(itemOne.id)
      loadItem(itemOne.id).map(_.collectionId) must_== Some(archiveCollectionId)
    }
    "throw an exception when item does not exist" in new scope {
      service.moveItemToArchive(randomItemId) must throwA[Throwable]
    }
    "return the archive collection id" in new scope {
      service.moveItemToArchive(itemOne.id) must_== Some(archiveCollectionId)
    }
  }

  "publish" should {
    "set item.published to true" in new scope {
      service.publish(itemOne.id)
      loadItem(itemOne.id).map(_.published) must_== Some(true)
    }
    "throw an exception when item does not exist" in new scope {
      service.publish(randomItemId) must throwA[Throwable]
    }
    "return true, if update is successful" in new scope {
      service.publish(itemOne.id) must_== true
    }
    "return true, if isPublished was true already" in new scope {
      service.publish(itemOne.id) must_== true
      service.publish(itemOne.id) must_== true
    }
    //TODO Cannot easily be tested without mocking dao
    "return false, if item could not be updated" in pending
  }

  "purge" should {
    "delete item from current" in new scope {
      service.purge(itemOne.id)
      loadItem(itemOne.id) must_== None
    }
    "delete item from old versions" in new scope {
      service.save(itemOne, createNewVersion = true)
      service.purge(itemOne.id)
      loadItem(itemOne.id) must_== None
    }
    "return Success when item has been deleted" in new scope {
      service.purge(itemOne.id) must_== Success(itemOne.id)
    }
    "return Success when item has not been deleted" in new scope {
      //TODO Maybe an error would be more appropriate?
      val id = randomItemId
      service.purge(id) must_== Success(id)
    }
  }

  "removeFileFromPlayerDefinition" should {
    trait removeFileFromPlayerDefinition extends scope

    "remove file from playerDefinition.files" in new removeFileFromPlayerDefinition {
      val file = StoredFile("name.png", "image/png", false)
      service.addFileToPlayerDefinition(itemOne.id, file)
      loadItem(itemOne.id).map(_.playerDefinition.get.files) must_== Some(Seq(file))
      service.removeFileFromPlayerDefinition(itemOne.id, file)
      loadItem(itemOne.id).map(_.playerDefinition.get.files) must_== Some(Seq())
    }

    "return true when call was successful" in new removeFileFromPlayerDefinition {
      val file = StoredFile("name.png", "image/png", false)
      service.removeFileFromPlayerDefinition(itemOne.id, file) match {
        case Success(res) => res must_== true
        case Failure(e) => failure(s"Unexpected error $e")
      }
    }
    //TODO Do we want it to throw?
    "throw error when item cannot be found" in new removeFileFromPlayerDefinition {
      val file = StoredFile("name.png", "image/png", false)
      service.removeFileFromPlayerDefinition(randomItemId, file) must throwA[Throwable]
    }
  }

  "saveNewUnpublishedVersion" should {
    "create new unpublished item when item is in current" in new scope {
      val res = service.saveNewUnpublishedVersion(itemOne.id)
      res.isDefined must_== true
      res must_!= Some(itemOne.id)

    }
    "throw exception when item has old version" in new scope {
      val oldId = itemOne.id
      service.save(itemOne, createNewVersion = true).toOption.get
      service.saveNewUnpublishedVersion(oldId) must throwA[Throwable]
    }

    "return None if the item cannot be found in current or archive" in new scope {
      service.saveNewUnpublishedVersion(randomItemId) must_== None
    }
  }

  "isAuthorizedBatch" should {

    trait isAuthorizedBatch extends scope {

      type Params = (VersionedId[ObjectId], Permission, Boolean)

      val otherOrg = insertOrg("other-org")
      val collectionTwo = insertCollection("two", otherOrg, isPublic = true)
      val otherItemOne = addItem(2, collectionTwo, Some(1))
    }

    "return 1 authorized result" in new isAuthorizedBatch {
      service.isAuthorizedBatch(org.id, (itemOne.id, Permission.Read)) must equalTo(Seq(itemOne.id -> Permission.Read -> true)).await
    }

    "return 2 results for same id with different permissions" in new isAuthorizedBatch {
      val params: Seq[Params] = Seq(
        (itemOne.id, Permission.Read, true),
        (itemOne.id, Permission.Write, true))

      service.isAuthorizedBatch(org.id, params.map(t => t._1 -> t._2): _*) must equalTo(
        params.map(t => t._1 -> t._2 -> t._3)).await
    }

    "return 2 results" in new isAuthorizedBatch {
      val random = randomItemId

      val params = Seq(
        (itemOne.id, Permission.Read, true),
        (random, Permission.Read, false))

      service.isAuthorizedBatch(org.id, params.map(t => t._1 -> t._2): _*) must equalTo(
        params.map(t => t._1 -> t._2 -> t._3)).await
    }

    "return 2 results for Write/Read on a public collection" in new isAuthorizedBatch {
      val params = Seq(
        (otherItemOne.id, Permission.Write, false),
        (otherItemOne.id, Permission.Read, true))

      service.isAuthorizedBatch(org.id, params.map(t => t._1 -> t._2): _*) must equalTo(
        params.map(t => t._1 -> t._2 -> t._3)).await
    }

    "return 4 results" in new isAuthorizedBatch {
      val random = randomItemId

      val params = Seq(
        (itemOne.id, Permission.Read, true),
        (otherItemOne.id, Permission.Read, true),
        (otherItemOne.id, Permission.Write, false),
        (random, Permission.Read, false))

      service.isAuthorizedBatch(org.id, params.map(t => t._1 -> t._2): _*) must equalTo(
        params.map(t => t._1 -> t._2 -> t._3)).await
    }
  }

  "findMultiplePlayerDefinitions" should {
    trait findMultiplePlayerDefinitions extends scope {

      val orgItem = addItem(3, collectionOne)
      val otherOrg = insertOrg("other-org")
      val collectionTwo = insertCollection("two", otherOrg)
      val otherItemOne = addItem(2, collectionTwo, Some(1))
      val randomItemIdOne = randomItemId
    }

    "return a single player definitions" in new findMultiplePlayerDefinitions {
      service.findMultiplePlayerDefinitions(org.id, itemOne.id) must equalTo(Seq(
        itemOne.id -> Success(itemOne.playerDefinition.get))).await
    }

    "return multiple player definitions" in new findMultiplePlayerDefinitions {
      service.findMultiplePlayerDefinitions(org.id, itemOne.id, orgItem.id) must equalTo(Seq(
        itemOne.id -> Success(itemOne.playerDefinition.get),
        orgItem.id -> Success(itemOne.playerDefinition.get))).await
    }

    "return a multiple player definitions with one access failure" in new findMultiplePlayerDefinitions {
      service.findMultiplePlayerDefinitions(org.id, itemOne.id, otherItemOne.id) must equalTo(Seq(
        otherItemOne.id -> Failure(PlatformServiceError("Not authorized to access")),
        itemOne.id -> Success(itemOne.playerDefinition.get))).await
    }

    "return a multiple player definitions with one access failure" in new findMultiplePlayerDefinitions {
      service.findMultiplePlayerDefinitions(org.id, itemOne.id, randomItemIdOne) must equalTo(Seq(
        randomItemIdOne -> Failure(PlatformServiceError("Not authorized to access")),
        itemOne.id -> Success(itemOne.playerDefinition.get))).await
    }
  }

}
