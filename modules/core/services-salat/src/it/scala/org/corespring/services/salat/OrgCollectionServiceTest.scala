package org.corespring.services.salat

import org.bson.types.ObjectId
import org.corespring.models.auth.Permission
import org.corespring.models.{ ContentCollection, ContentCollRef, Organization }
import org.corespring.services.errors.PlatformServiceError
import org.specs2.specification.BeforeAfter

import scalaz.{ Failure, Success }

class OrgCollectionServiceTest extends ServicesSalatIntegrationTest {

  trait scope extends BeforeAfter {

    def mkOrg(name: String) = Organization(name)

    lazy val service = services.orgCollectionService
    lazy val orgId: ObjectId = ObjectId.get
    lazy val collectionId: ObjectId = ObjectId.get
    lazy val collection = services.contentCollectionService.insertCollection(ContentCollection(name = "test-collection", ownerOrgId = orgId)).toOption.get
    lazy val contentCollRef = ContentCollRef(collectionId = collectionId, Permission.Read.value, enabled = true)
    val org = services.orgService.insert(Organization(name = "orgservice-test-org", id = orgId, contentcolls = Seq(contentCollRef)), None).toOption.get
    lazy val setId: ObjectId = ObjectId.get

    def insertOrg(name: String, parentId: Option[ObjectId] = None) = services.orgService.insert(mkOrg(name), parentId).toOption.get

    def after: Any = removeAllData()

    def before: Any = {}
  }

  "upsertAccessToCollection" should {

    trait upsert extends scope {

      def p: Permission
      val newCollectionId = ObjectId.get
      val expected = {
        val org = services.orgService.findOneById(orgId).get
        org.copy(contentcolls = org.contentcolls :+ ContentCollRef(newCollectionId, p.value, true))
      }
      lazy val result = service.upsertAccessToCollection(orgId, newCollectionId, Permission.Read)
    }

    trait readUpsert extends upsert {
      override def p: Permission = Permission.Read
    }

    trait writeUpsert extends upsert {
      override def p: Permission = Permission.Write
    }

    "read: return the updated org" in new readUpsert {
      result must_== Success(expected)
    }

    "read: read is allowed" in new readUpsert {
      service.isAuthorized(orgId, newCollectionId, Permission.Read) must_== true
    }

    "read: write is not allowed" in new readUpsert {
      service.isAuthorized(orgId, newCollectionId, Permission.Write) must_== false
    }

    "write: return the updated org" in new writeUpsert {
      result must_== Success(expected)
    }

    "write: read is allowed" in new writeUpsert {
      service.isAuthorized(orgId, newCollectionId, Permission.Read) must_== true
    }

    "write: write is allowed" in new writeUpsert {
      service.isAuthorized(orgId, newCollectionId, Permission.Write) must_== true
    }

    "update an existing ref if it already exists" in new scope {

      val newCollectionId = ObjectId.get
      val ref = ContentCollRef(newCollectionId, Permission.Read.value, true)
      services.orgService.findOneById(orgId).map { o =>
        val update = o.copy(contentcolls = o.contentcolls :+ ref)
        services.orgService.save(update)
      }

      val result = service.upsertAccessToCollection(orgId, newCollectionId, Permission.Write)
      val colls = result.fold(_ => Seq.empty, o => o.contentcolls)
      colls must_== Seq(ref.copy(pval = Permission.Write.value))
    }

  }

  "isAuthorized" should {
    trait isAuthorized extends scope {

      val testOrg = Organization("test owner of public collection")
      val publicCollection = ContentCollection("test public collection", testOrg.id, isPublic = true)
      val ownWritableCollection = ContentCollection("own writable collection", org.id, isPublic = false)
      val testOrgWritableCollection = ContentCollection("test org writable collection", testOrg.id, isPublic = false)

      override def before = {
        super.before
        services.orgService.insert(testOrg, None)
        services.contentCollectionService.insertCollection(publicCollection)
        services.contentCollectionService.insertCollection(testOrgWritableCollection)
        services.contentCollectionService.insertCollection(ownWritableCollection)
      }

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
        super.before
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
        super.before
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

  "getOrCreateDefaultCollection" should {
    "create new default collection, if it does not exist" in new scope {
      val dummyId = ObjectId.get
      service.getOrCreateDefaultCollection(org.id).map(c => c.copy(id = dummyId)) must_== Success(ContentCollection(ContentCollection.Default, org.id, false, dummyId))
    }

    "return default collection, if it does exist" in new scope {
      val existing = ContentCollection(ContentCollection.Default, org.id, id = ObjectId.get)
      services.contentCollectionService.insertCollection(existing)
      service.getOrCreateDefaultCollection(org.id).map(_.id) must_== Success(existing.id)
    }

    "fail if org does not exist" in new scope {
      service.getOrCreateDefaultCollection(ObjectId.get) must_== Failure(_: PlatformServiceError)
    }
  }

  "upsertAccessToCollection" should {
    trait getOrgs extends scope {
      val inserted = insertOrg("test org 2", None)
      service.upsertAccessToCollection(inserted.id, collectionId, Permission.Read)
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
      service.removeAccessToCollection(org.id, collectionId) must_== Success()
      service.isAuthorized(org.id, collectionId, Permission.Read) must_== false
    }

    "fail when org does not exist" in new scope {
      service.removeAccessToCollection(ObjectId.get, collectionId) must_== Failure(_: PlatformServiceError)
    }

    "not fail, when collection does not exist" in new scope {
      service.removeAccessToCollection(org.id, ObjectId.get) must_== Success()
    }

  }

  "ownsCollection" should {
    "should return Success when org owns collection" in new scope {
      service.ownsCollection(org, collection.id).isSuccess must_== true
    }

    "should return Failure when org does not own collection" in new scope {
      service.ownsCollection(Organization("some-org"), collection.id).isFailure must_== true
    }

    "should return Failure when collection does not exist" in new scope {
      service.ownsCollection(org, ObjectId.get).isFailure must_== true
    }
  }
}
