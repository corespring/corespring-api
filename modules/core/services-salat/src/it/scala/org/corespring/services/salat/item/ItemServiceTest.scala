package org.corespring.services.salat.item

import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId
import org.corespring.models.{ Organization, ContentCollection }
import org.corespring.models.auth.Permission
import org.corespring.models.item.resource.{ CloneFileResult, Resource, StoredFile }
import org.corespring.models.item._
import org.corespring.platform.data.mongo.exceptions.SalatVersioningDaoException
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.ContentCollectionService
import org.corespring.services.salat.ServicesSalatIntegrationTest
import org.joda.time.DateTime
import org.specs2.matcher.MatchResult
import org.specs2.mock.Mockito
import org.specs2.mutable.{ After, BeforeAfter }
import org.specs2.specification.Scope

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scalaz.{ Failure, Success, Validation }

class ItemServiceTest extends ServicesSalatIntegrationTest with Mockito {

  val itemService = services.itemService

  trait TestScope extends BeforeAfter {
    lazy val org = addOrg(1)

    def before:Any = {

    }

    def after:Any = {
      items.valuesIterator.foreach(i => itemService.purge(i.id))
      colls.valuesIterator.foreach(c => services.contentCollectionService.delete(c.id))
      orgs.valuesIterator.foreach(o => services.orgService.delete(o.id))
    }

    val orgs = new mutable.HashMap[Int, Organization]
    val colls = new mutable.HashMap[Int, ContentCollection]
    val items = new mutable.HashMap[Int, Item]

    def addOrg(id: Int): Organization = {
      val org = Organization("org-" + id)
      services.orgService.insert(org, None)
      orgs.put(id, org)
      org
    }

    def addCollection(id: Int, p: Permission = Permission.Read, enabled: Boolean = true) = {
      val coll = ContentCollection("test-coll-" + id, org.id)
      services.contentCollectionService.insertCollection(org.id, coll, p, enabled)
      colls.put(id, coll)
      coll
    }

    def addItem(id: Int, c: ContentCollection,
                contributorId: Option[Int] = None,
                contentType:Option[String] = None) = {
      val contributorDetails = ContributorDetails(
        contributor = Some("contributor-" + contributorId.getOrElse(id)))
      val item = Item(
        collectionId = c.id.toString,
        contributorDetails = Some(contributorDetails),
        contentType = contentType.getOrElse(Item.contentType))
      services.itemService.insert(item)
      items.put(id, item)
      item
    }

    def assertDbItem(id: VersionedId[ObjectId])(block: Item => Any) = {
      itemService.findOneById(id) match {
        case None => failure(s"Cannot find item $id")
        case Some(item) => block(item)
      }
    }
  }

  "addCollectionIdToSharedCollections" should {
    trait AddCollectionIdToSharedCollectionsScope extends TestScope {
        addCollection(1)
        addItem(1, colls(1))
        addItem(2, colls(1))
        val sharedCollectionId = addCollection(2).id
    }

    "add collectionId to item.sharedInCollections" in new AddCollectionIdToSharedCollectionsScope {
      itemService.addCollectionIdToSharedCollections(Seq(items(1).id, items(2).id), sharedCollectionId)
      assertDbItem(items(1).id) { item => item.sharedInCollections.contains(sharedCollectionId) }
      assertDbItem(items(2).id) { item => item.sharedInCollections.contains(sharedCollectionId) }
    }
    "return ids of updated items when call was successful" in new AddCollectionIdToSharedCollectionsScope {
      itemService.addCollectionIdToSharedCollections(Seq(items(1).id, items(2).id), sharedCollectionId) match {
        case Success(updatedItems) => updatedItems === Seq(items(1).id, items(2).id)
        case Failure(e) => failure(s"Unexpected error $e")
      }
    }
    //TODO How to make the dao fail?
    "return failed items when call failed for at least one item" in pending
  }

  "addFileToPlayerDefinition" should {
    trait AddFileToPlayerDefinitionScope extends TestScope {
      addCollection(1)
      addItem(1, colls(1))
    }
    "add file to playerDefinition.files using item id" in new AddFileToPlayerDefinitionScope {
      val file = StoredFile("name.png", "image/png", false)
      itemService.addFileToPlayerDefinition(items(1).id, file)
      assertDbItem(items(1).id) { _.playerDefinition.get.files === Seq(file) }
    }

    "add file to playerDefinition.files using item" in new AddFileToPlayerDefinitionScope {
      val file = StoredFile("name.png", "image/png", false)
      itemService.addFileToPlayerDefinition(items(1), file)
      assertDbItem(items(1).id) { _.playerDefinition.get.files === Seq(file) }
    }
    "return true when call was successful" in new AddFileToPlayerDefinitionScope {
      val file = StoredFile("name.png", "image/png", false)
      itemService.addFileToPlayerDefinition(items(1), file) match {
        case Success(res) => res === true
        case Failure(e) => failure(s"Unexpected error $e")
      }
    }
    //TODO Do we want it to throw?
    "throw error when item cannot be found" in new AddFileToPlayerDefinitionScope {
      val file = StoredFile("name.png", "image/png", false)
      itemService.addFileToPlayerDefinition(VersionedId(ObjectId.get), file) must throwA[SalatVersioningDaoException]
    }
  }

  "asMetadataOnly" should {
    trait AsMetadataOnlyScope extends Scope {
      val longAgo = new DateTime(1000, 10, 10, 10, 10)
      val item = new Item(
        id = VersionedId(ObjectId.get),
        supportingMaterials = Seq(Resource(name = "test", files = Seq.empty)),
        data = Some(Resource(name = "test-data", files = Seq.empty)),
        collectionId = "1234567",
        dateModified = Some(longAgo))
      def assertDatetimeEquals(d1: DateTime, d2: DateTime, precisionMillis: Int = 1000 * 60) = {
        def mins(d: DateTime) = Math.floor(d.getMillis() / precisionMillis)
        mins(d1) == mins(d2)
      }
    }
    "set dateModified to the current time" in new AsMetadataOnlyScope {
      val res = itemService.asMetadataOnly(item)
      assertDatetimeEquals(new DateTime(res.get("dateModified")), DateTime.now())
    }
    "remove id from result" in new AsMetadataOnlyScope {
      val res = itemService.asMetadataOnly(item)
      res.containsField("id") === false
    }
    "remove supportingMaterials from result" in new AsMetadataOnlyScope {
      val res = itemService.asMetadataOnly(item)
      res.containsField("supportingMaterials") === false
    }
    "remove data from result" in new AsMetadataOnlyScope {
      val res = itemService.asMetadataOnly(item)
      res.containsField("data") === false
    }
    "remove collectionId from result" in new AsMetadataOnlyScope {
      val res = itemService.asMetadataOnly(item)
      res.containsField("collectionId") === false
    }
  }

  "clone" should {
    trait CloneScope extends TestScope {
      val item = Item(collectionId = "1234567")
      val clonedItem = itemService.clone(item)

      override def after = {
        clonedItem.map(itemService.purge(_))
        super.after
      }
    }
    "return the cloned item" in new CloneScope{
      clonedItem.get.id !== item.id
    }
    "create a new item in the db" in new CloneScope{
      val dbItem = itemService.findOneById(clonedItem.get.id)
      dbItem.isDefined === true
    }
    //TODO How much of file cloning do we want to test?
    "clone stored files" in pending
  }

  "collectionIdForItem" should {
    trait CollectionIdForItemScope extends BeforeAfter {
      val itemId = ObjectId.get
      val v0CollectionId = ObjectId.get
      val v0Item = Item(id = VersionedId(itemId, Some(0)), collectionId = v0CollectionId.toString)
      val v1CollectionId = ObjectId.get
      val v1Item = Item(id = VersionedId(itemId, Some(1)), collectionId = v1CollectionId.toString)

      override def before = {
        itemService.insert(v1Item)
      }

      override def after = {
        itemService.purge(v1Item.id)
      }
    }

    "return the collectionId of the item" in new CollectionIdForItemScope {
      val res = itemService.collectionIdForItem(v1Item.id)
      res === Some(v1CollectionId)
    }
    "always return the collectionId of the last version of the item" in new CollectionIdForItemScope {
      val res = itemService.collectionIdForItem(v0Item.id)
      res === Some(v1CollectionId)
    }
    "return None if item does not exist" in new CollectionIdForItemScope {
      val res = itemService.collectionIdForItem(VersionedId(ObjectId.get))
      res === None
    }
    "return None if collectionId is not an ObjectId" in new CollectionIdForItemScope {
      val itemWithInvalidCollectionId = Item(collectionId = "this is not an ObjectId")
      itemService.insert(itemWithInvalidCollectionId) !== None
      val res = itemService.collectionIdForItem(itemWithInvalidCollectionId.id)
      res === None
      itemService.purge(itemWithInvalidCollectionId.id)
    }
  }

  "contributorsForOrg" should {

    "return single contributor from one collection for an org" in new TestScope {
      addCollection(1)
      addItem(1, colls(1))

      val res = itemService.contributorsForOrg(org.id)
      res === Seq("contributor-1")
    }
    "return multiple contributors from one collection for an org" in new TestScope {
      addCollection(1)
      addItem(1, colls(1))
      addItem(2, colls(1))

      val res = itemService.contributorsForOrg(org.id)
      res === Seq("contributor-1", "contributor-2")
    }
    "return multiple contributors from multiple collection for an org" in new TestScope {
      addCollection(1)
      addItem(1, colls(1))
      addCollection(2)
      addItem(2, colls(2))

      val res = itemService.contributorsForOrg(org.id)
      res === Seq("contributor-1", "contributor-2")
    }
    "not include a contributor more than once" in new TestScope {
      addCollection(1)
      addItem(1, colls(1), contributorId=Some(77))
      addCollection(2)
      addItem(2, colls(2), contributorId=Some(77))

      val res = itemService.contributorsForOrg(org.id)
      res === Seq("contributor-77")
    }
    //TODO enabled: should we return items from collections which are not enabled
    "include contributors from collection even though it is not enabled" in new TestScope {
      addCollection(1, enabled = false)
      addItem(1, colls(1))

      val res = itemService.contributorsForOrg(org.id)
      res === Seq("contributor-1")
    }
    "not include contributors from collections which are not readable" in new TestScope {
      addCollection(1, p = Permission.None)
      addItem(1, colls(1))

      val res = itemService.contributorsForOrg(org.id)
      res === Seq.empty
    }
    "not include contributors from archived items" in new TestScope {
      addCollection(1)
      addItem(1, colls(1))
      services.itemService.moveItemToArchive(items(1).id)

      val res = itemService.contributorsForOrg(org.id)
      res === Seq.empty
    }
    "not include contributors from items in versionedContent" in new TestScope {
      addCollection(1)
      addItem(1, colls(1))
      val updatedItem = items(1).copy(
        contributorDetails=Some(ContributorDetails(contributor=Some("updated contributor"))))
      itemService.save(updatedItem, createNewVersion=true)

      //the versioned item has the old contributor still
      assertDbItem(items(1).id){
        _.contributorDetails.get.contributor === Some("contributor-1")
      }

      val res = itemService.contributorsForOrg(org.id)
      res === Seq("updated contributor")
    }
  }

  "count" should {
    "return 0, if no item matches the query" in new TestScope {
      addCollection(1)
      addItem(1, colls(1))
      addItem(2, colls(1))
      itemService.count(MongoDBObject("impossible.mission" -> "no way")) === 0
    }
    "return the number of all items when query is empty" in new TestScope {
      addCollection(1)
      addItem(1, colls(1))
      addItem(2, colls(1))
      itemService.count(MongoDBObject()) === 2
    }
    "return the number of items which match the query" in new TestScope {
      addCollection(1)
      addItem(1, colls(1))
      addItem(2, colls(1))
      itemService.count(MongoDBObject("contributorDetails.contributor" -> "contributor-1")) === 1
    }
    "return the number of items with contentType item only" in new TestScope {
      addCollection(1)
      addItem(1, colls(1), contentType=Some(Item.contentType))
      addItem(2, colls(1), contentType=Some("thing"))

      itemService.count(MongoDBObject()) === 1
    }
  }

  "countItemsInCollection" should {
    "return number of items in collection" in new TestScope {
      addCollection(1)
      addItem(1, colls(1))
      addItem(2, colls(1))
      itemService.countItemsInCollection(colls(1).id) === 2
    }
    //TODO Do we want to count items with contentType different from item?
    "return number of things in collection even though contentType is not item" in new TestScope {
      addCollection(1)
      addItem(1, colls(1), contentType=Some("apple"))
      addItem(2, colls(1), contentType=Some("pear"))
      itemService.countItemsInCollection(colls(1).id) === 2
    }
    "return 0 if collection does not exist" in new TestScope {
      itemService.countItemsInCollection(ObjectId.get) === 0
    }
    "return 0 if collection does not contain any item" in new TestScope {
      addCollection(1)
      itemService.countItemsInCollection(colls(1).id) === 0
    }
  }

  "currentVersion" should {
    "return 0 as the the current version of a new item" in new TestScope {
      addCollection(1)
      addItem(1, colls(1))
      itemService.currentVersion(items(1).id) === 0
    }
    "return 1 as the the current version of an updated item" in new TestScope {
      addCollection(1)
      addItem(1, colls(1))
      itemService.save(items(1), true)
      itemService.currentVersion(items(1).id) === 1
    }
    //TODO Shouldn't that result in an error?
    "return 0 for a non existing item" in new TestScope {
      itemService.currentVersion(VersionedId(ObjectId.get)) === 0
    }
  }

  "deleteFromSharedCollections" should { "work" in pending }
  "find" should { "work" in pending }
  "findFieldsById" should { "work" in pending }
  "findItemStandards" should { "work" in pending }
  "findMultiple" should { "work" in pending }
  "findMultipleById" should { "work" in pending }
  "findOne" should { "work" in pending }
  "findOneById" should { "work" in pending }
  "getOrCreateUnpublishedVersion" should { "work" in pending }
  "getQtiXml" should { "work" in pending }
  "insert" should { "work" in pending }
  "isPublished" should { "work" in pending }
  "moveItemToArchive" should { "work" in pending }
  "publish" should { "work" in pending }
  "purge" should { "work" in pending }
  "removeCollectionIdsFromShared" should { "work" in pending }
  "save" should { "work" in pending }
  "saveNewUnpublishedVersion" should { "work" in pending }
  "saveUsingDbo" should { "work" in pending }

  "findItemStandards" should {
    trait scope extends After {

      lazy val item = Item(
        collectionId = ObjectId.get.toString,
        taskInfo = Some(TaskInfo(title = Some("title"))),
        standards = Seq("S1", "S2"))
      lazy val itemId = services.itemService.insert(item).get

      override def after: Any = {
        services.itemService.purge(itemId)
      }
    }

    "return an item standard" in new scope {
      services.itemService.findItemStandards(itemId) must_== Some(ItemStandards("title", Seq("S1", "S2"), itemId))
    }
  }

  "save" should {

    def mockAssets(succeed: Boolean) = {
      val m = mock[ItemAssetService]

      m.cloneStoredFiles(any[Item], any[Item]).answers { (args, _) =>
        {
          println(s" --> args:  $args")
          val out: Validation[Seq[CloneFileResult], Item] = if (succeed) {
            val arr = args.asInstanceOf[Array[Any]]
            println(s" --> arr: $arr")
            Success(arr(1).asInstanceOf[Item])
          } else {
            Failure(Seq.empty[CloneFileResult])
          }
          out
        }
      }
      m
    }

    def itemServiceWithMockFiles(succeed: Boolean) = new ItemService(
      itemService.asInstanceOf[ItemService].dao,
      mockAssets(succeed),
      mock[ContentCollectionService],
      services.context,
      services.archiveConfig) {
    }

    def assertSaveWithStoredFile(name: String, shouldSucceed: Boolean): MatchResult[Any] = {
      val service = itemServiceWithMockFiles(shouldSucceed)
      val id = VersionedId(ObjectId.get)
      val file = StoredFile(name, "image/png", false, StoredFile.storageKey(id.id, 0, "data", name))
      val resource = Resource(name = "data", files = Seq(file))
      val item = Item(id = id, collectionId = "?", data = Some(resource), taskInfo = Some(TaskInfo(title = Some("original title"))))
      val latestId = service.insert(item)

      latestId.map { vid =>
        vid.version === Some(0)
      }.getOrElse(failure("insert failed"))

      val update = item.copy(id = latestId.get, taskInfo = Some(TaskInfo(title = Some("new title"))))

      service.save(update, true)

      val dbItem = service.findOneById(VersionedId(item.id.id))

      val expectedVersion = if (shouldSucceed) 1 else 0

      println("expecting version: " + expectedVersion)

      val out: MatchResult[Any] = dbItem
        .map(i => i.id === VersionedId(id.id, Some(expectedVersion))).get

      out
    }

    "revert the version if a failure occurred when cloning stored files" in {
      assertSaveWithStoredFile("bad.png", false)
    }

    "update the version if no failure occurred when cloning stored files" in {
      assertSaveWithStoredFile("good.png", true)
    }
  }

}
