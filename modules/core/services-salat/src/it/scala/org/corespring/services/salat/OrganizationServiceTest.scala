package org.corespring.services.salat

import org.bson.types.ObjectId
import org.corespring.models.auth.Permission
import org.corespring.models.{ ContentCollRef, ContentCollection, MetadataSetRef, Organization }
import org.corespring.services.errors.{ GeneralError, PlatformServiceError }
import org.specs2.mock.Mockito
import org.specs2.mutable.BeforeAfter

import scalaz.{ Failure, Success }

class OrganizationServiceTest extends ServicesSalatIntegrationTest with Mockito {

  trait scope extends BeforeAfter {

    def mkOrg(name: String) = Organization(name)

    lazy val service = services.orgService
    lazy val orgId: ObjectId = ObjectId.get
    lazy val collectionId: ObjectId = ObjectId.get
    lazy val contentCollRef = ContentCollRef(collectionId = collectionId, Permission.Read.value, enabled = true)
    val org = service.insert(Organization(name = "orgservice-test-org", id = orgId, contentcolls = Seq(contentCollRef)), None).toOption.get
    lazy val setId: ObjectId = ObjectId.get

    def insertOrg(name: String, parentId: Option[ObjectId] = None) = service.insert(mkOrg(name), parentId).toOption.get

    def after: Any = {
      service.delete(orgId)
      removeAllData()
    }

    def before: Any = {}
  }

  "addCollection" should {

    "add contentColRef with collection id and read permission" in new scope {
      val newCollectionId = ObjectId.get
      service.addCollection(orgId, newCollectionId, Permission.Read) must_== Success(ContentCollRef(newCollectionId, Permission.Read.value, false))
      service.canAccessCollection(orgId, newCollectionId, Permission.Read) must_== true
      service.canAccessCollection(orgId, newCollectionId, Permission.Write) must_== false
    }

    "add contentColRef with collection id and write permission" in new scope {
      val newCollectionId = ObjectId.get
      service.addCollection(orgId, newCollectionId, Permission.Write) must_== Success(ContentCollRef(newCollectionId, Permission.Write.value, false))
    }

    "fail to add contentColRef if it exists already" in new scope {
      service.addCollection(orgId, collectionId, Permission.Read).isFailure must_== true
    }

    //TODO Do we really want to add two content coll refs with same collection but different permissions?
    //Ed: I think we should fail if this happens and add an updateCollection function -- or an updateCollection with an 'upsert' option a la mongo.
    "allow to add contentColRef with existing collection id but different perms" in new scope {
      service.addCollection(orgId, collectionId, Permission.Write) must_== Success(ContentCollRef(collectionId, Permission.Write.value, false))
    }
  }

  "addCollectionReference" should {
    "add content coll reference to org" in new scope {
      service.getPermissions(orgId, newCollectionId) must_== None
      val newCollectionId = ObjectId.get
      val ref = ContentCollRef(newCollectionId, Permission.Write.value)
      service.addCollectionReference(orgId, ref) must_== Success()
      service.getPermissions(orgId, newCollectionId) must_== Some(Permission.Write)
    }

    //TODO: If the ref is adding - is should update the ref that's there instead
    "not change the permission when content coll ref is duplicate" in new scope {
      val existingPermission = service.getPermissions(orgId, collectionId)
      existingPermission must_!= Some(Permission.Write)
      val ref = ContentCollRef(collectionId, Permission.Write.value)
      service.addCollectionReference(orgId, ref) must_== Success()
      service.getPermissions(orgId, collectionId) must_== existingPermission
    }
  }

  "addMetadataSet" should {

    "return the new ref" in new scope {
      service.addMetadataSet(orgId, setId) must_== Success(MetadataSetRef(setId, true))
    }

    "add a metadataset to the org" in new scope {
      service.addMetadataSet(orgId, setId)
      service.findOneById(orgId).map(_.metadataSets) must_== Some(Seq(MetadataSetRef(setId, true)))
    }
  }

  "contentCollectionService.insertCollection" should {
    trait insertCollection extends scope {
      val testOrg = Organization("test owner of public collection")

      val publicCollection = ContentCollection("test public collection", testOrg.id, true, id = ObjectId.get)

      override def before = {
        super.before
        service.insert(testOrg, None)
      }

      override def after = {
        service.delete(testOrg.id)
        super.after
      }
    }

    "give read access to all orgs" in new insertCollection {

      services.contentCollectionService.insertCollection(testOrg.id, publicCollection, Permission.Write)

      forall(service.list()) { o =>
        val hasAccess = service.canAccessCollection(o, publicCollection.id, Permission.Read)
        hasAccess must_== true
      }

      val isCollectionListed = services.contentCollectionService
        .listAllCollectionsAvailableForOrg(org.id)
        .exists(i => i.contentCollection.id == publicCollection.id)
      isCollectionListed must_== true
    }

    "succeed adding any id as public collection" in new insertCollection {
      service.addPublicCollectionToAllOrgs(ObjectId.get) must_== Failure(_: PlatformServiceError)
    }
  }

  "canAccessCollection" should {
    trait canAccessCollection extends scope {

      val testOrg = Organization("test owner of public collection")
      val publicCollection = ContentCollection("test public collection", testOrg.id, isPublic = true)
      val ownWritableCollection = ContentCollection("own writable collection", org.id, isPublic = false)
      val testOrgWritableCollection = ContentCollection("test org writable collection", testOrg.id, isPublic = false)

      override def before = {
        super.before
        service.insert(testOrg, None)
        services.contentCollectionService.insertCollection(testOrg.id, publicCollection, Permission.Write, true)
        services.contentCollectionService.insertCollection(testOrg.id, testOrgWritableCollection, Permission.Write, true)
        services.contentCollectionService.insertCollection(org.id, ownWritableCollection, Permission.Write, true)
      }

      def canAccess(collection: ContentCollection, p: Permission): Boolean = {
        canAccess(collection.id, p)
      }

      def canAccess(id: ObjectId, p: Permission): Boolean = {
        service.canAccessCollection(org.id, id, p)
      }
    }

    "return true when accessing public collection with read permissions" in new canAccessCollection {
      canAccess(publicCollection, Permission.Read) must_== true
    }

    "return false when accessing public collection with write permissions" in new canAccessCollection {
      canAccess(publicCollection, Permission.Write) must_== false
    }
    "return true when accessing own readable collection with read permissions" in new canAccessCollection {
      canAccess(collectionId, Permission.Read) must_== true
    }
    "return false when accessing own readable collection with write permissions" in new canAccessCollection {
      canAccess(collectionId, Permission.Write) must_== false
    }
    "return true when accessing own writable collection with read permissions" in new canAccessCollection {
      canAccess(ownWritableCollection.id, Permission.Read) must_== true
    }
    "return true when accessing own writable collection with write permissions" in new canAccessCollection {
      canAccess(ownWritableCollection.id, Permission.Write) must_== true
    }
    "return false when accessing collection of other org with read permission" in new canAccessCollection {
      canAccess(testOrgWritableCollection.id, Permission.Read) must_== false
    }
    "return false when accessing collection of other org with write permission" in new canAccessCollection {
      canAccess(testOrgWritableCollection.id, Permission.Write) must_== false
    }
  }

  "delete" should {
    trait delete extends scope {
      val childOrgOne = insertOrg("child org one", Some(org.id))
      val childOrgTwo = insertOrg("child org two", Some(org.id))
    }

    "remove the org itself" in new delete {
      service.findOneById(org.id) must_== Some(_: Organization)
      service.delete(org.id)
      service.findOneById(org.id) must_== None
    }

    "remove all the child orgs too" in new delete {
      service.findOneById(childOrgOne.id) must_== Some(_: Organization)
      service.findOneById(childOrgTwo.id) must_== Some(_: Organization)
      service.delete(org.id)
      service.findOneById(childOrgOne.id) must_== None
      service.findOneById(childOrgTwo.id) must_== None
    }

    "not fail if the org does not exist" in new delete {
      service.delete(ObjectId.get) must_== Success()
    }
  }

  "deleteCollectionFromAllOrganizations" should {
    trait deleteCollection extends scope {
      val testOrg = new Organization("test org",
        contentcolls = Seq(ContentCollRef(collectionId = collectionId)))

      override def before = {
        super.before
        service.insert(testOrg, None)
      }
    }

    "remove the collection from all orgs" in new deleteCollection {
      service.getPermissions(org.id, collectionId) must_== Some(Permission.Read)
      service.getPermissions(testOrg.id, collectionId) must_== Some(Permission.Read)

      service.deleteCollectionFromAllOrganizations(collectionId)

      service.getPermissions(org.id, collectionId) must_== None
      service.getPermissions(testOrg.id, collectionId) must_== None
    }
  }

  //TODO How are using enabled?
  "disableCollection" should {
    trait DisableCollectionScope extends scope {

      override def before: Unit = {
        super.before
        service.disableCollection(org.id, collectionId)
      }
    }

    "disable collection for org" in new DisableCollectionScope {
      service.disableCollection(org.id, collectionId) must_== Success(ContentCollRef(collectionId, Permission.Read.value, false))
    }

    "fail if org does not exist" in new DisableCollectionScope {
      service.disableCollection(ObjectId.get, collectionId) must_== Failure(_: PlatformServiceError)
    }

    "fail if collection does not exist" in new DisableCollectionScope {
      service.disableCollection(org.id, ObjectId.get) must_== Failure(_: PlatformServiceError)
    }
  }

  //TODO How are using enabled?
  "enableCollection" should {
    trait EnableCollectionScope extends scope {

      override def before: Unit = {
        super.before
        service.disableCollection(org.id, collectionId)
      }
    }

    "enable collection for org" in new EnableCollectionScope {
      service.enableCollection(org.id, collectionId) must_== Success(ContentCollRef(collectionId, Permission.Read.value, true))
    }

    "fail if org does not exist" in new EnableCollectionScope {
      service.enableCollection(ObjectId.get, collectionId) must_== Failure(_: PlatformServiceError)
    }

    "fail if collection does not exist" in new EnableCollectionScope {
      service.enableCollection(org.id, ObjectId.get) must_== Failure(_: PlatformServiceError)
    }
  }

  "findOneById" should {
    "return Some(org) if it can be found" in new scope {
      service.findOneById(org.id) must_== Some(org)
    }

    "return None if org does not exist" in new scope {
      service.findOneById(ObjectId.get) must_== None
    }
  }

  "findOneByName" should {
    //TODO Should insert check if a name is used already?
    "return first org, that hasbeen inserted with a name" in new scope {
      val org1 = service.insert(Organization("X"), None).toOption.get
      val org2 = service.insert(Organization("X"), None).toOption.get
      org1.id !== org2.id
      service.findOneByName("X") must_== Some(org1)
    }

    "return Some(org), if it can be found" in new scope {
      service.findOneByName(org.name) must_== Some(org)
    }

    "return None, if name is not in db" in new scope {
      service.findOneByName("non existent org name") must_== None
    }
  }

  "getDefaultCollection" should {
    "create new default collection, if it does not exist" in new scope {
      val dummyId = ObjectId.get
      service.getOrCreateDefaultCollection(org.id).map(c => c.copy(id = dummyId)) must_== Success(ContentCollection(OrganizationService.Keys.DEFAULT, org.id, false, dummyId))
    }

    "return default collection, if it does exist" in new scope {
      val existing = ContentCollection(OrganizationService.Keys.DEFAULT, org.id, id = ObjectId.get)
      services.contentCollectionService.insertCollection(org.id, existing, Permission.Write, enabled = true)
      service.getOrCreateDefaultCollection(org.id).map(_.id) must_== Success(existing.id)
    }

    "fail if org does not exist" in new scope {
      service.getOrCreateDefaultCollection(ObjectId.get) must_== Failure(_: PlatformServiceError)
    }
  }

  "getOrgsWithAccessTo" should {
    trait getOrgs extends scope {
      val inserted = insertOrg("test org 2", None)
      service.addCollection(inserted.id, collectionId, Permission.Read)
      val newOrg = service.findOneById(inserted.id).get
    }

    "return all orgs with access to collection" in new getOrgs {
      service.getOrgsWithAccessTo(collectionId) must_== Stream(org, newOrg)
    }

    "return empty seq if no org has access to collection" in new getOrgs {
      service.getOrgsWithAccessTo(ObjectId.get) must_== Stream.empty
    }

  }

  "getTree" should {
    trait getTree extends scope {
      val childOne = insertOrg("childOne", Some(org.id))
      val childTwo = insertOrg("childTwo", Some(org.id))
      val grandChild = insertOrg("grandChild", Some(childTwo.id))
    }

    "return empty seq when org does not exist" in new getTree {
      service.getTree(ObjectId.get) must_== Seq.empty
    }

    "return just org when it does not have any children" in new getTree {
      service.getTree(childOne.id) must_== Seq(childOne)
    }

    "return org and and child" in new getTree {
      service.getTree(childTwo.id) must_== Seq(childTwo, grandChild)
    }

    "return org, child and grand child" in new getTree {
      service.getTree(org.id) must_== Seq(org, childOne, childTwo, grandChild)
    }
  }

  "insert" should {
    trait insert extends scope {
      val childOrg = insertOrg("child", Some(org.id))
    }

    "add the org to the db" in new insert {
      service.findOneById(childOrg.id) must_== Some(childOrg)
    }
    "add org's own id to the paths if parent is None" in new insert {
      service.findOneById(org.id).map(_.path) must_== Some(Seq(org.id))
    }

    "add the parent's id to the paths if parent is not None" in new insert {
      service.findOneById(childOrg.id).map(_.path) must_== Some(Seq(childOrg.id, org.id))
    }
  }

  "orgsWithPath" should {

    trait orgsWithPath extends scope {
      val childOrg = insertOrg("child", Some(org.id))
      val grandChildOrg = insertOrg("grand child", Some(childOrg.id))
    }

    "return parent only, when deep = false" in new orgsWithPath {
      service.orgsWithPath(org.id, deep = false) must_== Seq(org)
    }
    "return parent and child, when deep = true" in new orgsWithPath {
      service.orgsWithPath(childOrg.id, deep = true) must_== Seq(childOrg, grandChildOrg)
    }
    "also returns deeply nested orgs (> one level), when deep = true" in new orgsWithPath {
      service.orgsWithPath(org.id, deep = true) must_== Seq(org, childOrg, grandChildOrg)
    }
  }

  "removeCollection" should {
    "remove collection from org" in new scope {
      service.canAccessCollection(org.id, collectionId, Permission.Read) must_== true
      service.removeCollection(org.id, collectionId) must_== Success()
      service.canAccessCollection(org.id, collectionId, Permission.Read) must_== false
    }

    "fail when org does not exist" in new scope {
      service.removeCollection(ObjectId.get, collectionId) must_== Failure(_: PlatformServiceError)
    }

    "not fail, when collection does not exist" in new scope {
      service.removeCollection(org.id, ObjectId.get) must_== Success()
    }

    //TODO Do we really want to be able to remove a public collection from an org?
    //TODO That seems to be inconsistent with hasAccessToCollection
    "allow to remove a public collection" in new scope {
      val publicOrg = Organization("Public")
      val publicCollection = ContentCollection("public", publicOrg.id, isPublic = true)
      services.contentCollectionService.insertCollection(publicOrg.id, publicCollection, Permission.Write, true)
      service.addPublicCollectionToAllOrgs(publicCollection.id)
      service.removeCollection(org.id, publicCollection.id)
      service.getPermissions(org.id, publicCollection.id) must_== None
    }
  }

  "removeMetadataSet" should {

    "remove a metadataset" in new scope {
      service.addMetadataSet(orgId, setId)
      service.removeMetadataSet(orgId, setId)
      service.findOneById(org.id).map(_.metadataSets) must_== Some(Seq.empty)
    }

    "return the metadataset, which has been removed" in new scope {
      service.addMetadataSet(orgId, setId)
      service.removeMetadataSet(orgId, setId) must_== Success(MetadataSetRef(setId, true))
    }

    "fail when org cannot be found" in new scope {
      service.removeMetadataSet(ObjectId.get, setId) must_== Failure(_: PlatformServiceError)
    }

    "fail when metadata set cannot be found" in new scope {
      service.removeMetadataSet(org.id, ObjectId.get) must_== Failure(_: GeneralError)
    }
  }

}

