package org.corespring.services.salat.item

import org.bson.types.ObjectId
import org.corespring.models.{Organization, ContentCollection}
import org.corespring.models.auth.Permission
import org.corespring.models.item.resource.{CloneFileResult, Resource, StoredFile}
import org.corespring.models.item.{Item, ItemStandards, PlayerDefinition, TaskInfo}
import org.corespring.platform.data.mongo.exceptions.SalatVersioningDaoException
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.ContentCollectionService
import org.corespring.services.salat.ServicesSalatIntegrationTest
import org.joda.time.DateTime
import org.specs2.matcher.MatchResult
import org.specs2.mock.Mockito
import org.specs2.mutable.{After, BeforeAfter}
import org.specs2.specification.Scope

import scalaz.{Failure, Success, Validation}

class ItemServiceTest extends ServicesSalatIntegrationTest with Mockito {

  val itemService = services.itemService

  trait TestScope extends BeforeAfter {

    val itemOne = Item("Item One",
      data = Some(Resource(name = "data", files = Seq.empty)),
      playerDefinition = Some(PlayerDefinition.empty))
    val itemTwo = Item("Item Two")

    lazy val collectionId = ObjectId.get

    override def before = {
      itemService.insert(itemOne)
      itemService.insert(itemTwo)
    }

    override def after = {
      itemService.purge(itemOne.id)
      itemService.purge(itemTwo.id)
    }

    def assertDbItem(id: VersionedId[ObjectId])(block: Item => Any) = {
      itemService.findOneById(id) match {
        case None => failure(s"Cannot find item $id")
        case Some(item) => block(item)
      }
    }
  }

  "addCollectionIdToSharedCollections" should {
    "add collectionId to item.sharedInCollections" in new TestScope {
      itemService.addCollectionIdToSharedCollections(Seq(itemOne.id, itemTwo.id), collectionId)
      assertDbItem(itemOne.id) { item => item.sharedInCollections.contains(collectionId) }
      assertDbItem(itemTwo.id) { item => item.sharedInCollections.contains(collectionId) }
    }
    "return ids of updated items when call was successful" in new TestScope {
      itemService.addCollectionIdToSharedCollections(Seq(itemOne.id, itemTwo.id), collectionId) match {
        case Success(items) => items === Seq(itemOne.id, itemTwo.id)
        case Failure(e) => failure(s"Unexpected error $e")
      }
    }
    //TODO How to make the dao fail?
    "return failed items when call failed for at least one item" in pending
  }

  "addFileToPlayerDefinition" should {
    "add file to playerDefinition.files using item id" in new TestScope {
      val file = StoredFile("name.png", "image/png", false)
      itemService.addFileToPlayerDefinition(itemOne.id, file)
      assertDbItem(itemOne.id) { _.playerDefinition.get.files === Seq(file)}
    }

    "add file to playerDefinition.files using item" in new TestScope {
      val file = StoredFile("name.png", "image/png", false)
      itemService.addFileToPlayerDefinition(itemOne, file)
      assertDbItem(itemOne.id) { _.playerDefinition.get.files === Seq(file)}
    }
    "return true when call was successful" in new TestScope {
      val file = StoredFile("name.png", "image/png", false)
      itemService.addFileToPlayerDefinition(itemOne, file) match {
        case Success(res) => res === true
        case Failure(e) => failure(s"Unexpected error $e")
      }
    }
    //TODO Do we want it to throw?
    "throw error when item cannot be found" in new TestScope {
      val file = StoredFile("name.png", "image/png", false)
      itemService.addFileToPlayerDefinition(VersionedId(ObjectId.get), file) must throwA[SalatVersioningDaoException]
    }
  }

  "asMetadataOnly" should {
    trait localScope extends Scope {
      val longAgo = new DateTime(1000, 10, 10, 10, 10)
      val item = new Item(
        id = VersionedId(ObjectId.get),
        supportingMaterials = Seq(Resource(name="test", files=Seq.empty)),
        data = Some(Resource(name="test-data", files=Seq.empty)),
        collectionId = "1234567",
        dateModified = Some(longAgo)
      )
      def assertDatetimeEquals(d1:DateTime, d2: DateTime, precisionMillis:Int = 1000*60)= {
        def mins(d:DateTime) = Math.floor(d.getMillis() / precisionMillis)
        mins(d1) == mins(d2)
      }
    }
    "set dateModified to the current time" in new localScope {
      val res = itemService.asMetadataOnly(item)
      assertDatetimeEquals(new DateTime(res.get("dateModified")), DateTime.now())
    }
    "remove id from result" in new localScope {
      val res = itemService.asMetadataOnly(item)
      res.containsField("id") === false
    }
    "remove supportingMaterials from result" in new localScope {
      val res = itemService.asMetadataOnly(item)
      res.containsField("supportingMaterials") === false
    }
    "remove data from result"in new localScope {
      val res = itemService.asMetadataOnly(item)
      res.containsField("data") === false
    }
    "remove collectionId from result"in new localScope {
      val res = itemService.asMetadataOnly(item)
      res.containsField("collectionId") === false
    }
  }

  "clone" should {
    "return the cloned item" in {
      val item = Item(collectionId = "1234567")
      val clonedItem = itemService.clone(item)
      clonedItem.get.id !== item.id
    }
    "create a new item in the db" in {
      val item = Item(collectionId = "1234567")
      val clonedItem = itemService.clone(item)
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
      val v0Item = Item(id=VersionedId(itemId, Some(0)), collectionId=v0CollectionId.toString)
      val v1CollectionId = ObjectId.get
      val v1Item = Item(id=VersionedId(itemId, Some(1)), collectionId=v1CollectionId.toString)

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
      val itemWithInvalidCollectionId = Item(collectionId="this is not an ObjectId")
      itemService.insert(itemWithInvalidCollectionId) !== None
      val res = itemService.collectionIdForItem(itemWithInvalidCollectionId.id)
      res === None
    }
  }

  "contributorsForOrg" should {
    "return contributors for an org" in new Scope {
      val org = Organization("test-org")
      val collection = ContentCollection("test", org.id)
      val item = Item(collectionId="")
    }

    "not include contributors from archived items" in pending
    "not include a contributor more than once" in pending
    "not include contributors from items in versionedContent" in pending
  }

  "count" should { "work" in pending }
  "countItemsInCollection" should { "work" in pending }
  "currentVersion" should { "work" in pending }
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
