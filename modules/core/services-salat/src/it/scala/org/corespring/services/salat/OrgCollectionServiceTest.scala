package org.corespring.services.salat

import org.bson.types.ObjectId
import org.corespring.models.auth.Permission
import org.corespring.models.{ CollectionInfo, ContentCollection, ContentCollRef, Organization }
import org.corespring.services.errors.PlatformServiceError
import org.specs2.mutable.{ BeforeAfter, After }
import org.specs2.specification.{ Scope }

import scalaz.{ Failure, Success }

class OrgCollectionServiceTest extends ServicesSalatIntegrationTest {

  trait scope extends BeforeAfter with Scope with InsertionHelper {
    lazy val service = services.orgCollectionService
    lazy val orgId: ObjectId = ObjectId.get
    lazy val collectionId: ObjectId = ObjectId.get
    lazy val collection = services.contentCollectionService.insertCollection(ContentCollection(name = "test-collection", ownerOrgId = orgId, id = collectionId)).toOption.get
    lazy val contentCollRef = ContentCollRef(collectionId = collectionId, Permission.Read.value, enabled = true)
    val org = services.orgService.insert(Organization(name = "orgservice-test-org", id = orgId, contentcolls = Seq(contentCollRef)), None).toOption.get
    lazy val setId: ObjectId = ObjectId.get

    override def before: Any = {}

    override def after: Any = removeAllData()
  }

  "grantAccessToCollection" should {

    trait grant extends scope {

      def p: Permission
      val newCollectionId = ObjectId.get
      val expected = {
        val org = services.orgService.findOneById(orgId).get
        org.copy(contentcolls = org.contentcolls :+ ContentCollRef(newCollectionId, p.value, true))
      }
      logger.debug(s"call upsert with: newCollectionId=$newCollectionId")
      val result = service.grantAccessToCollection(orgId, newCollectionId, p)
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

    "read: read is allowed" in new grantRead {
      service.isAuthorized(orgId, newCollectionId, Permission.Read) must_== true
    }

    "read: write is not allowed" in new grantRead {
      service.isAuthorized(orgId, newCollectionId, Permission.Write) must_== false
    }

    "write: return the updated org" in new grantWrite {
      result must_== Success(expected)
    }

    "write: read is allowed" in new grantWrite {
      service.isAuthorized(orgId, newCollectionId, Permission.Read) must_== true
    }

    "write: write is allowed" in new grantWrite {
      service.isAuthorized(orgId, newCollectionId, Permission.Write) must_== true
    }

    "update an existing ref if it already exists" in new scope {

      val newCollectionId = ObjectId.get
      val ref = ContentCollRef(newCollectionId, Permission.Read.value, true)
      services.orgService.findOneById(orgId).map { o =>
        val update = o.copy(contentcolls = o.contentcolls :+ ref)
        services.orgService.save(update)
      }

      val result = service.grantAccessToCollection(orgId, newCollectionId, Permission.Write)
      val colls = result.fold(_ => Seq.empty, o => o.contentcolls)
      colls.find(_.collectionId == ref.collectionId) must_== Some(ref.copy(pval = Permission.Write.value))
    }

  }

  "isAuthorized" should {
    trait isAuthorized extends scope {

      val testOrg = insertOrg("test owner of public collection")
      val publicCollection = insertCollection("test public collection", testOrg, true)
      val ownWritableCollection = insertCollection("own writable collection", org)
      val testOrgWritableCollection = insertCollection("test org writable collection", testOrg)

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

    "return true when accessing own readable collection with read permissions" in new isAuthorized {
      isAuthorized(collectionId, Permission.Read) must_== true
    }

    "return false when accessing own readable collection with write permissions" in new isAuthorized {
      isAuthorized(collectionId, Permission.Write) must_== false
    }

    "return true when accessing own writable collection with read permissions" in new isAuthorized {
      isAuthorized(ownWritableCollection.id, Permission.Read) must_== true
    }

    "return true when accessing own writable collection with write permissions" in new isAuthorized {
      isAuthorized(ownWritableCollection.id, Permission.Write) must_== true
    }

    "return false when accessing collection of other org with read permission" in new isAuthorized {
      isAuthorized(testOrgWritableCollection.id, Permission.Read) must_== false
    }

    "return false when accessing collection of other org with write permission" in new isAuthorized {
      isAuthorized(testOrgWritableCollection.id, Permission.Write) must_== false
    }
  }

  "enableCollection" should {
    trait enable extends scope {

      override def before: Unit = {
        service.disableCollection(org.id, collectionId)
      }
    }

    "enable collection for org" in new enable {
      service.enableCollection(org.id, collectionId) must_== Success(ContentCollRef(collectionId, Permission.Read.value, true))
    }

    "fail if org does not exist" in new enable {
      service.enableCollection(ObjectId.get, collectionId) must_== Failure(_: PlatformServiceError)
    }

    "fail if collection does not exist" in new enable {
      service.enableCollection(org.id, ObjectId.get) must_== Failure(_: PlatformServiceError)
    }
  }

  //TODO How are using enabled?
  "disableCollection" should {
    trait disable extends scope {

      override def before: Unit = {
        service.disableCollection(org.id, collectionId)
      }
    }

    "disable collection for org" in new disable {
      service.disableCollection(org.id, collectionId) must_== Success(ContentCollRef(collectionId, Permission.Read.value, false))
    }

    "fail if org does not exist" in new disable {
      service.disableCollection(ObjectId.get, collectionId) must_== Failure(_: PlatformServiceError)
    }

    "fail if collection does not exist" in new disable {
      service.disableCollection(org.id, ObjectId.get) must_== Failure(_: PlatformServiceError)
    }
  }

  "getDefaultCollection" should {
    "create new default collection, if it does not exist" in new scope {
      val dummyId = ObjectId.get
      service.getDefaultCollection(org.id).map(c => c.copy(id = dummyId)) must_== Success(ContentCollection(ContentCollection.Default, org.id, false, dummyId))
    }

    "return default collection, if it does exist" in new scope {
      val existing = ContentCollection(ContentCollection.Default, org.id, id = ObjectId.get)
      services.contentCollectionService.insertCollection(existing)
      service.getDefaultCollection(org.id).map(_.id) must_== Success(existing.id)
    }

    "fail if org does not exist" in new scope {
      service.getDefaultCollection(ObjectId.get) must_== Failure(_: PlatformServiceError)
    }
  }

  "getOrgsWithAccessTo" should {
    trait getOrgs extends scope {
      val inserted = insertOrg("test org 2", None)
      service.grantAccessToCollection(inserted.id, collectionId, Permission.Read)
      val newOrg = services.orgService.findOneById(inserted.id).get
    }

    "return all orgs with access to collection" in new getOrgs {
      service.getOrgsWithAccessTo(collectionId) must_== Stream(org, newOrg)
    }

    "return empty seq if no org has access to collection" in new getOrgs {
      service.getOrgsWithAccessTo(ObjectId.get) must_== Stream.empty
    }

  }

  "removeAccessToCollection" should {
    "remove collection from org" in new scope {
      service.isAuthorized(org.id, collectionId, Permission.Read) must_== true
      service.removeAccessToCollection(org.id, collectionId) must_== Success(org.copy(contentcolls = Seq.empty))
      service.isAuthorized(org.id, collectionId, Permission.Read) must_== false
    }

    "fail when org does not exist" in new scope {
      service.removeAccessToCollection(ObjectId.get, collectionId) must_== Failure(_: PlatformServiceError)
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
      val orgOne = services.orgService.insert(Organization(id = ObjectId.get, name = "org-one"), None).toOption.get
      val otherOrg = services.orgService.insert(Organization(id = ObjectId.get, name = "other-org"), None).toOption.get
      val writeOne = insertCollection("write-one", orgOne, false)
      val readOne = insertCollection("read-one", orgOne, false)
      val publicOne = insertCollection("public", orgOne, true)
      val otherOrgColl = insertCollection("other-org-col", otherOrg, false)
      services.orgCollectionService.grantAccessToCollection(orgOne.id, otherOrgColl.id, Permission.Read)

      override def after: Any = removeAllData()
    }

    "list all the collections for org" in new listAllCollectionsAvailableForOrg {
      lazy val result = service.listAllCollectionsAvailableForOrg(orgOne.id).toSeq
      result must_== Stream(
        CollectionInfo(writeOne, 0, orgOne.id, Permission.Write),
        CollectionInfo(readOne, 0, orgOne.id, Permission.Write),
        CollectionInfo(publicOne, 0, orgOne.id, Permission.Write),
        CollectionInfo(otherOrgColl, 0, orgOne.id, Permission.Read))
    }

    "list public collections for other org" in new listAllCollectionsAvailableForOrg {
      val orgTwo = services.orgService.insert(Organization(id = ObjectId.get, name = "org-two"), None).toOption.get
      lazy val result = service.listAllCollectionsAvailableForOrg(orgTwo.id)
      result must_== Stream(
        CollectionInfo(publicOne, 0, orgTwo.id, Permission.Read))
    }
  }

  "getPermission" should {

    trait getPermission extends scope {

      val root = insertOrg("root")
      val child = insertOrg("child", Some(root.id))
      val otherOrg = insertOrg("otherOrg")
      val rootOne = insertCollection("one", root)
      val otherOrgOne = insertCollection("other-org-one", otherOrg)
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
      service.removeAccessToCollectionForAllOrgs(one.id)
      service.isAuthorized(childOne.id, one.id, Permission.Read) must_== false
    }

    "not remove childOne's access to the collection if it's public" in new removeAccessToAll {
      services.orgCollectionService.grantAccessToCollection(childOne.id, publicOne.id, Permission.Write)
      service.isAuthorized(childOne.id, publicOne.id, Permission.Read) must_== true
      service.isAuthorized(childOne.id, publicOne.id, Permission.Write) must_== true
      service.removeAccessToCollectionForAllOrgs(publicOne.id)
      service.isAuthorized(childOne.id, publicOne.id, Permission.Read) must_== true
      service.isAuthorized(childOne.id, publicOne.id, Permission.Write) must_== false
    }
  }

  "getCollections" should {

    trait getCollections extends scope {
      val one = insertCollection("one", org, false)
      val childOrg = insertOrg("child", Some(org.id))
      val childOne = insertCollection("child-one", childOrg, false)
      val otherOrg = insertOrg("other-org", None)
      val otherOrgColl = insertCollection("other-org-coll", otherOrg, false)
      services.orgCollectionService.grantAccessToCollection(childOrg.id, otherOrgColl.id, Permission.Read)
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
