package org.corespring.services.salat

import grizzled.slf4j.Logger
import org.bson.types.ObjectId
import org.corespring.errors.PlatformServiceError
import org.corespring.models.auth.Permission
import org.corespring.models.{ CollectionInfo, ContentCollRef, ContentCollection, Organization }
import org.corespring.services.CollectionIdPermission
import org.specs2.mutable.{ After, BeforeAfter }
import org.specs2.specification.Scope

import scala.concurrent.Await
import scala.concurrent.duration._
import scalaz.{ Failure, Success }

class OrgCollectionServiceTest extends ServicesSalatIntegrationTest {

  private lazy val testLogger = Logger(classOf[OrgCollectionServiceTest])

  trait scope extends BeforeAfter with Scope with InsertionHelper {
    lazy val service = services.orgCollectionService
    val org = insertOrg("org-test")
    lazy val collection = insertCollection("test-coll", org, false)
    lazy val setId: ObjectId = ObjectId.get

    override def before: Any = {}

    override def after: Any = removeAllData()
  }

  "grandAccessToCollection + list" should {

    trait grantAndList extends scope {
      def p: Permission
      val otherOrg = insertOrg("other-org")
      val newCollection = insertCollection("new-collection", otherOrg)
      val newCollectionId = newCollection.id
      testLogger.debug(s"grant: org=${org.id}, permission=$p, collection=$newCollectionId")
      val result = service.grantAccessToCollection(org.id, newCollectionId, p)
    }

    "list should return updated collection with clone permission" in new grantAndList {
      override def p = Permission.Clone
      lazy val collections = service.listAllCollectionsAvailableForOrg(org.id, 0, 0)
      collections must equalTo(Stream(CollectionInfo(newCollection, 0, org.id, p))).await
    }

    "list should return updated collection with read permission" in new grantAndList {
      override def p = Permission.Read
      lazy val collections = service.listAllCollectionsAvailableForOrg(org.id, 0, 0)
      collections must equalTo(Stream(CollectionInfo(newCollection, 0, org.id, p))).await
    }

    "list should return updated collection with write permission" in new grantAndList {
      override def p = Permission.Write
      lazy val collections = service.listAllCollectionsAvailableForOrg(org.id, 0, 0)
      collections must equalTo(Stream(CollectionInfo(newCollection, 0, org.id, p))).await
    }
  }

  "grantAccessToCollection" should {

    trait grant extends scope {

      def p: Permission
      val otherOrg = insertOrg("other-org")
      val newCollection = insertCollection("new-collection", otherOrg)
      val newCollectionId = newCollection.id
      val expected = {
        val o = services.orgService.findOneById(org.id).get
        o.copy(contentcolls = org.contentcolls :+ ContentCollRef(newCollectionId, p.value, true))
      }
      logger.debug(s"call upsert with: newCollectionId=$newCollectionId")
      val result = service.grantAccessToCollection(org.id, newCollectionId, p)
    }

    class grantRead extends grant {
      override def p: Permission = Permission.Read
    }

    class grantWrite extends grant {
      override def p: Permission = Permission.Write
    }

    "read: return the updated org" in new grantRead {
      result must_== Success(expected)
    }

    "returns a failure if the collectionId doesn't belong to any collection" in new scope {
      val randomCollectionId = ObjectId.get
      service.grantAccessToCollection(org.id, randomCollectionId, Permission.Write) must_== Failure(_: PlatformServiceError)
    }

    "read: read is allowed" in new grantRead {
      service.isAuthorized(org.id, newCollectionId, Permission.Read) must_== true
    }

    "read: write is not allowed" in new grantRead {
      service.isAuthorized(org.id, newCollectionId, Permission.Write) must_== false
    }

    "write: return the updated org" in new grantWrite {
      result must_== Success(expected)
    }

    "write: read is allowed" in new grantWrite {
      service.isAuthorized(org.id, newCollectionId, Permission.Read) must_== true
    }

    "write: write is allowed" in new grantWrite {
      service.isAuthorized(org.id, newCollectionId, Permission.Write) must_== true
    }

    "update an existing ref if it already exists" in new scope {
      val otherOrg = insertOrg("other-org")
      val newCollection = insertCollection("new-collection", otherOrg)
      val newCollectionId = newCollection.id
      val ref = ContentCollRef(newCollectionId, Permission.Read.value, true)
      services.orgService.findOneById(org.id).map { o =>
        val update = o.copy(contentcolls = o.contentcolls :+ ref)
        services.orgService.save(update)
      }

      val result = service.grantAccessToCollection(org.id, newCollectionId, Permission.Write)
      val colls = result.fold(_ => Seq.empty, o => o.contentcolls)
      colls.find(_.collectionId == ref.collectionId) must_== Some(ref.copy(pval = Permission.Write.value))
    }

  }

  "isAuthorizedBatch" should {

    trait isAuthorized extends scope {
      val testOrg = insertOrg("test owner of public collection")
      val testOrgOne = insertCollection("test org writable collection", testOrg)
      val publicCollection = insertCollection("test public collection", testOrg, true)
      val otherOrg = insertOrg("other-org")
      val otherOrgOne = insertCollection("other-org-one", otherOrg)
      giveOrgAccess(org, otherOrgOne, Permission.Read)

    }

    "return 1 result" in new isAuthorized {
      val result = service.isAuthorizedBatch(testOrg.id, CollectionIdPermission(publicCollection.id, Permission.Read))
      result must equalTo(Seq(CollectionIdPermission(publicCollection.id, Permission.Read) -> true)).await
    }

    "return 2 results" in new isAuthorized {

      val expected = Seq(
        CollectionIdPermission(publicCollection.id, Permission.Read) -> true,
        CollectionIdPermission(otherOrgOne.id, Permission.Write) -> false)

      service.isAuthorizedBatch(testOrg.id,
        expected.map(_._1): _*) must equalTo(expected).await
    }

    "return results for the same collection id" in new isAuthorized {
      service.isAuthorizedBatch(org.id,
        CollectionIdPermission(otherOrgOne.id, Permission.Read),
        CollectionIdPermission(otherOrgOne.id, Permission.Write)) must equalTo(
          Seq(
            CollectionIdPermission(otherOrgOne.id, Permission.Read) -> true,
            CollectionIdPermission(otherOrgOne.id, Permission.Write) -> false)).await
    }

    "return 3 results" in new isAuthorized {
      service.isAuthorizedBatch(testOrg.id,
        CollectionIdPermission(publicCollection.id, Permission.Read),
        CollectionIdPermission(otherOrgOne.id, Permission.Write),
        CollectionIdPermission(collection.id, Permission.Write)) must equalTo(
          Seq(
            CollectionIdPermission(publicCollection.id, Permission.Read) -> true,
            CollectionIdPermission(otherOrgOne.id, Permission.Write) -> false,
            CollectionIdPermission(collection.id, Permission.Write) -> false)).await
    }
  }

  "isAuthorized" should {
    trait isAuthorized extends scope {

      val testOrg = insertOrg("test owner of public collection")
      val testOrgOne = insertCollection("test org writable collection", testOrg)

      val publicCollection = insertCollection("test public collection", testOrg, true)

      val otherOrg = insertOrg("other-org")
      val otherOrgOne = insertCollection("other-org-one", otherOrg)

      giveOrgAccess(org, otherOrgOne, Permission.Read)

      def isAuthorized(collection: ContentCollection, p: Permission): Boolean = {
        isAuthorized(collection.id, p)
      }

      def isAuthorized(id: ObjectId, p: Permission): Boolean = {
        service.isAuthorized(org.id, id, p)
      }
    }

    "return true when accessing public collection with read permissions" in new isAuthorized {
      isAuthorized(publicCollection, Permission.Read) must_== true
    }

    "return false when accessing public collection with write permissions" in new isAuthorized {
      isAuthorized(publicCollection, Permission.Write) must_== false
    }

    "return true when accessing collection with read permissions" in new isAuthorized {
      isAuthorized(collection.id, Permission.Read) must_== true
    }

    "return true when accessing collection with write permissions" in new isAuthorized {
      isAuthorized(collection.id, Permission.Write) must_== true
    }

    "return false when accessing a collection that the org has read access to" in new isAuthorized {
      isAuthorized(otherOrgOne.id, Permission.Write) must_== false
    }

    "return true when accessing a collection that the org has read access to" in new isAuthorized {
      isAuthorized(otherOrgOne.id, Permission.Read) must_== true
    }

    "return false when accessing collection of other org with read permission" in new isAuthorized {
      isAuthorized(testOrgOne.id, Permission.Read) must_== false
    }

    "return false when accessing collection of other org with write permission" in new isAuthorized {
      isAuthorized(testOrgOne.id, Permission.Write) must_== false
    }
  }

  "enableOrgAccessToCollection" should {
    trait enable extends scope {

      override def before: Unit = {
        service.disableOrgAccessToCollection(org.id, collection.id)
      }
    }

    "enable collection for org" in new enable {
      service.enableOrgAccessToCollection(org.id, collection.id) must_== Success(ContentCollRef(collection.id, Permission.Write.value, true))
    }

    "fail if org does not exist" in new enable {
      service.enableOrgAccessToCollection(ObjectId.get, collection.id) must_== Failure(_: PlatformServiceError)
    }

    "fail if collection does not exist" in new enable {
      service.enableOrgAccessToCollection(org.id, ObjectId.get) must_== Failure(_: PlatformServiceError)
    }
  }

  "disableOrgAccessToCollection" should {
    trait disable extends scope {

      override def before: Unit = {
        service.disableOrgAccessToCollection(org.id, collection.id)
      }
    }

    "disable collection for org" in new disable {
      service.disableOrgAccessToCollection(org.id, collection.id) must_== Success(ContentCollRef(collection.id, Permission.Write.value, false))
    }

    "fail if org does not exist" in new disable {
      service.disableOrgAccessToCollection(ObjectId.get, collection.id) must_== Failure(_: PlatformServiceError)
    }

    "fail if collection does not exist" in new disable {
      service.disableOrgAccessToCollection(org.id, ObjectId.get) must_== Failure(_: PlatformServiceError)
    }
  }

  "getDefaultCollection" should {
    "create new default collection, if it does not exist" in new scope {
      val dummyId = ObjectId.get
      service.getDefaultCollection(org.id).map(c => c.copy(id = dummyId)) must_== Success(ContentCollection(ContentCollection.Default, org.id, false, dummyId))
    }

    "return default collection, if it does exist" in new scope {
      val existing = insertCollection(ContentCollection.Default, org)
      service.getDefaultCollection(org.id).map(_.id) must_== Success(existing.id)
    }

    "fail if org does not exist" in new scope {
      service.getDefaultCollection(ObjectId.get) must_== Failure(_: PlatformServiceError)
    }
  }

  "getOrgsWithAccessTo" should {
    trait getOrgs extends scope {
      val inserted = insertOrg("test org 2", None)
      service.grantAccessToCollection(inserted.id, collection.id, Permission.Read)
      val newOrg = services.orgService.findOneById(inserted.id).get
    }

    "return all orgs with access to collection" in new getOrgs {
      service.getOrgsWithAccessTo(collection.id).map(_.name) must_== Stream(org, newOrg).map(_.name)
    }

    "return empty seq if no org has access to collection" in new getOrgs {
      service.getOrgsWithAccessTo(ObjectId.get) must_== Stream.empty
    }

  }

  "removeAccessToCollection" should {
    "remove collection from org" in new scope {
      service.isAuthorized(org.id, collection.id, Permission.Read) must_== true
      service.removeAccessToCollection(org.id, collection.id) must_== Success(org.copy(contentcolls = Seq.empty))
      service.isAuthorized(org.id, collection.id, Permission.Read) must_== false
    }

    "fail when org does not exist" in new scope {
      service.removeAccessToCollection(ObjectId.get, collection.id) must_== Failure(_: PlatformServiceError)
    }

    "not fail, when collection does not exist" in new scope {
      service.removeAccessToCollection(org.id, ObjectId.get) must_== Success(org)
    }

  }

  "ownsCollection" should {
    "should return Success when org owns collection" in new scope {
      service.ownsCollection(org, collection.id) must_== Success(true)
    }

    "should return Failure when org does not own collection" in new scope {
      service.ownsCollection(Organization("some-org"), collection.id).isFailure must_== true
    }

    "should return Failure when collection does not exist" in new scope {
      service.ownsCollection(org, ObjectId.get).isFailure must_== true
    }
  }

  "listAllCollectionsAvailableForOrg" should {

    trait listAllCollectionsAvailableForOrg
      extends Scope
      with InsertionHelper
      with After {
      val service = services.orgCollectionService
      val orgOne = insertOrg("org-one")
      val otherOrg = insertOrg("other-org")
      val writeOne = insertCollection("write-one", orgOne, false)
      val readOne = insertCollection("read-one", orgOne, false)
      val publicOne = insertCollection("public", orgOne, true)
      val otherOrgColl = insertCollection("other-org-col", otherOrg, false)
      services.orgCollectionService.grantAccessToCollection(orgOne.id, otherOrgColl.id, Permission.Read)

      override def after: Any = removeAllData()
    }

    "list all the collections for org" in new listAllCollectionsAvailableForOrg {
      lazy val result = waitFor(service.listAllCollectionsAvailableForOrg(orgOne.id, 0, 0)).toSeq
      result must_== Stream(
        CollectionInfo(writeOne, 0, orgOne.id, Permission.Write),
        CollectionInfo(readOne, 0, orgOne.id, Permission.Write),
        CollectionInfo(publicOne, 0, orgOne.id, Permission.Write),
        CollectionInfo(otherOrgColl, 0, orgOne.id, Permission.Read))
    }

    "list all the collections for org with limit of 1" in new listAllCollectionsAvailableForOrg {
      lazy val result = waitFor(service.listAllCollectionsAvailableForOrg(orgOne.id, 0, 1))
      result must_== Stream(CollectionInfo(writeOne, 0, orgOne.id, Permission.Write))
    }

    "list public collections for other org" in new listAllCollectionsAvailableForOrg {
      val orgTwo = insertOrg("org-two")
      lazy val result = waitFor(service.listAllCollectionsAvailableForOrg(orgTwo.id, 0, 0))
      result must_== Stream(
        CollectionInfo(publicOne, 0, orgTwo.id, Permission.Read))
    }

  }

  "getPermissions" should {
    trait getPermissions extends scope {
      val root = insertOrg("root")
      val child = insertOrg("child", Some(root.id))
      val otherOrg = insertOrg("otherOrg")
      val rootOne = insertCollection("one", root)
      val otherOrgOne = insertCollection("other-org-one", otherOrg)
    }

    "return n permissions of None for unknown org" in new getPermissions {
      val someOrgId = ObjectId.get
      val collectionIds = (1 to 50).map { i => ObjectId.get }
      val permissions = Await.result(service.getPermissions(someOrgId, collectionIds: _*), 2.seconds)
      permissions.length must_== 50
      permissions.filter(_._2.isDefined).length must_== 0
    }

    "return 1 permission for multiple duplicate ids" in new getPermissions {
      service.getPermissions(root.id, rootOne.id, rootOne.id, rootOne.id) must equalTo(Seq((rootOne.id -> Some(Permission.Write)))).await
    }

    "return 1 permission" in new getPermissions {
      service.getPermissions(root.id, rootOne.id) must equalTo(Seq((rootOne.id -> Some(Permission.Write)))).await
    }

    "return 2 permissions" in new getPermissions {
      service.getPermissions(root.id, rootOne.id, otherOrgOne.id) must equalTo(Seq(
        (rootOne.id -> Some(Permission.Write)),
        (otherOrgOne.id -> None))).await
    }

    "return 3 permissions" in new getPermissions {

      service.grantAccessToCollection(root.id, otherOrgOne.id, Permission.Clone)

      val randomId = ObjectId.get
      val permissions = Await.result(service.getPermissions(root.id, rootOne.id, otherOrgOne.id, randomId), 2.seconds)

      permissions.length must_== 3

      permissions must_== Seq(
        (rootOne.id -> Some(Permission.Write)),
        (otherOrgOne.id -> Some(Permission.Clone)),
        (randomId -> None))
    }
  }

  "getPermission" should {

    trait getPermission extends scope {

      val root = insertOrg("root")
      val child = insertOrg("child", Some(root.id))
      val otherOrg = insertOrg("otherOrg")
      val rootOne = insertCollection("one", root)
      val otherOrgOne = insertCollection("other-org-one", otherOrg)
      val archiveCollection = insertCollection("archive", root, false, id = archiveContentCollectionId)
    }

    "return Some(Read) permission for archived collection" in new getPermission {
      service.getPermission(org.id, archiveContentCollectionId) must_== Some(Permission.Read)
    }

    "return Permission.Write for a public collection that an org has write access to" in new getPermission {
      val publicCollection = insertCollection("public", root, true)
      service.getPermission(root.id, publicCollection.id) must_== Some(Permission.Write)
    }

    "return None for an unknown org and collection" in new getPermission {
      service.getPermission(ObjectId.get, ObjectId.get) must_== None
    }

    "return None for an unknown collection" in new getPermission {
      service.getPermission(root.id, ObjectId.get) must_== None
    }

    "return None for a collection belonging to another org" in new getPermission {
      service.getPermission(root.id, otherOrgOne.id) must_== None
    }

    "return Some(Read) for a public collection belonging to another org" in new getPermission {
      val otherOrgTwo = insertCollection("other-org-one", otherOrg, true)
      service.getPermission(root.id, otherOrgTwo.id) must_== Some(Permission.Read)
    }

    "return Some(Write) for a collection belonging to another org that this org has write access to" in new getPermission {
      services.orgCollectionService.grantAccessToCollection(root.id, otherOrgOne.id, Permission.Write)
      service.getPermission(root.id, otherOrgOne.id) must_== Some(Permission.Write)
    }

    "return Some(Read) for a collection belonging to another org that this org has read access to" in new getPermission {
      services.orgCollectionService.grantAccessToCollection(root.id, otherOrgOne.id, Permission.Read)
      service.getPermission(root.id, otherOrgOne.id) must_== Some(Permission.Read)
    }

    "return Some(Write) for a collection belonging the owner org" in new getPermission {
      service.getPermission(root.id, rootOne.id) must_== Some(Permission.Write)
    }

    "return Some(Write) for a default collection belonging the owner org" in new getPermission {
      val defaultColl = service.getDefaultCollection(root.id).toOption.get
      service.getPermission(root.id, defaultColl.id) must_== Some(Permission.Write)
    }
  }

  "removeAccessToCollection" should {

    trait removeAccessToAll extends scope {
      val one = insertCollection("one", org, false)
      val publicOne = insertCollection("public-one", org, true)
      val childOne = insertOrg("child-one", Some(org.id))
    }

    "remove childOne's access to the collection" in new removeAccessToAll {
      services.orgCollectionService.grantAccessToCollection(childOne.id, one.id, Permission.Read)
      service.isAuthorized(childOne.id, one.id, Permission.Read) must_== true
      service.removeAllAccessToCollection(one.id)
      service.isAuthorized(childOne.id, one.id, Permission.Read) must_== false
    }

    "not remove childOne's access to the collection if it's public" in new removeAccessToAll {
      val r = services.orgCollectionService.grantAccessToCollection(childOne.id, publicOne.id, Permission.Write)
      testLogger.debug(s"childOne id: ${childOne.id}")
      testLogger.debug(s"r: $r")
      service.isAuthorized(childOne.id, publicOne.id, Permission.Read) must_== true
      val perms = service.getPermission(childOne.id, publicOne.id)
      testLogger.debug(s"perms for childOne: $perms")
      service.isAuthorized(childOne.id, publicOne.id, Permission.Write) must_== true
      service.removeAllAccessToCollection(publicOne.id)
      service.isAuthorized(childOne.id, publicOne.id, Permission.Read) must_== true
      service.isAuthorized(childOne.id, publicOne.id, Permission.Write) must_== false
    }

    //TODO: Not allowing the owner org access to a collection they own sounds wrong. Check if this is correct
    "remove the ownerOrgs access to the collection" in new removeAccessToAll {
      service.removeAllAccessToCollection(one.id)
      service.isAuthorized(org.id, one.id, Permission.Read) must_== false
      service.ownsCollection(org, one.id) must_== Success(true)
    }
  }

  private case class OrgAndCol(org: Organization, coll: ContentCollection)

  "getCollections" should {

    trait getCollections extends scope {
      val one = insertCollection("one", org, false)
      val childOrg = insertOrg("child", Some(org.id))
      val childOne = insertCollection("child-one", childOrg, false)
      val otherOrg = insertOrg("other-org", None)
      val otherOrgColl = insertCollection("other-org-coll", otherOrg, false)
      services.orgCollectionService.grantAccessToCollection(childOrg.id, otherOrgColl.id, Permission.Read)
    }

    trait deepNesting extends scope {

      private def createNest(levels: Int) = {

        (1 to levels).foldLeft(Seq.empty[OrgAndCol]) { (acc, index) =>

          val parentOrg = if (acc.isEmpty) {
            None
          } else {
            Some(acc.last.org.id)
          }
          val o = insertOrg(s"level-$index", parentOrg)
          val c = insertCollection(s"coll-level-$index", o, false)
          acc :+ OrgAndCol(o, c)
        }
      }

      val orgAndColls = createNest(10)
    }

    "with 10 levels should return 10 collections" in new deepNesting {
      service.getCollections(orgAndColls(0).org.id, Permission.Write).map(_.size) must_== Success(10)
      service.getCollections(orgAndColls(0).org.id, Permission.Write) must_== Success(orgAndColls.map(_.coll).toStream)
    }

    "with 10 levels should return 5 collections for child 5" in new deepNesting {
      service.getCollections(orgAndColls(4).org.id, Permission.Write).map(_.size) must_== Success(6)
      service.getCollections(orgAndColls(4).org.id, Permission.Write) must_== Success(orgAndColls.drop(4).map(_.coll).toStream)
    }

    "with Permission.Write" should {
      "should return collections for org and childOrg" in new getCollections {
        service.getCollections(org.id, Permission.Write) must_== Success(Stream(one, childOne))
      }

      "should return collections for childOrg only" in new getCollections {
        service.getCollections(childOrg.id, Permission.Write) must_== Success(Stream(childOne))
      }
    }

    "with Permission.Read" should {

      "return all readable collections for org and childOrg" in new getCollections {
        service.getCollections(org.id, Permission.Read) must_== Success(Stream(one, childOne, otherOrgColl))
      }

      "return all readable collections for childOrg only" in new getCollections {
        service.getCollections(childOrg.id, Permission.Read) must_== Success(Stream(childOne, otherOrgColl))
      }

      "include public collections too" in new getCollections {
        val publicColl = insertCollection("other-org-public", otherOrg, true)
        service.getCollections(childOrg.id, Permission.Read) must_== Success(Stream(childOne, otherOrgColl, publicColl))
      }

    }
  }

}
