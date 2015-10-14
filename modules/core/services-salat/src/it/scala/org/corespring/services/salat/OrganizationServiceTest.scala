package org.corespring.services.salat

import org.bson.types.ObjectId
import org.corespring.models.auth.Permission
import org.corespring.models.metadata.MetadataSet
import org.corespring.models.{ ContentCollection, MetadataSetRef, ContentCollRef, Organization }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.errors.{ PlatformServiceError, GeneralError }
import org.specs2.matcher.{ Expectable, Matcher }
import org.specs2.mock.Mockito
import org.specs2.mutable.BeforeAfter

import scalaz.{ Validation, Failure, Success }

class OrganizationServiceTest extends ServicesSalatIntegrationTest with Mockito {

  trait TestScope extends BeforeAfter {

    lazy val service = services.orgService
    lazy val orgId: ObjectId = ObjectId.get
    lazy val collectionId: ObjectId = ObjectId.get
    lazy val contentCollRef = ContentCollRef(collectionId = collectionId, Permission.Read.value, enabled = true)
    lazy val org = Organization(name = "orgservice-test-org", id = orgId, contentcolls = Seq(contentCollRef))
    lazy val setId: ObjectId = ObjectId.get

    def after: Any = service.delete(orgId)

    def before: Any = service.insert(org, None)

    def assertContentCollRefs(ccr: ContentCollRef, collId: ObjectId, p: Permission, org: ObjectId) = {
      ccr.collectionId must_== collId
      ccr.pval must_== p.value
      service.hasCollRef(orgId, ccr) must_== true
    }

    def assertSeqOfOrgs(orgs: Seq[Organization], orgsToFind: Organization*) = {
      for (orgToFind <- orgsToFind) {
        if (orgs.find(o => o.id == orgToFind.id) == None) {
          failure(s"Expected to find ${orgToFind.name} in seq")
        }
      }
      orgsToFind.length must_== orgs.length
    }

    def assertContentCollRefEnabled(ref: ContentCollRef, expectedStatus: Boolean) = {
      ref.enabled must_== expectedStatus
      service.findOneById(org.id).get
        .contentcolls
        .find(_.collectionId == collectionId)
        .map(_.enabled) must_== Some(expectedStatus)
    }

    def orgExistsInDb(orgId: ObjectId) = {
      service.findOneById(org.id).isDefined
    }

    def assertDbOrg(orgId: ObjectId)(block: Organization => Unit) = {
      service.findOneById(orgId) match {
        case None => failure(s"org not found: $orgId")
        case Some(org) => block(org)
      }
    }

  }

  "addCollection" should {

    "add contentColRef with collection id and read permission" in new TestScope {
      val newCollectionId = ObjectId.get
      service.addCollection(orgId, newCollectionId, Permission.Read) must_== Success(ContentCollRef(newCollectionId, Permission.Read.value, false))
      service.canAccessCollection(orgId, newCollectionId, Permission.Read) must_== true
      service.canAccessCollection(orgId, newCollectionId, Permission.Write) must_== false
    }

    "add contentColRef with collection id and write permission" in new TestScope {
      val newCollectionId = ObjectId.get
      service.addCollection(orgId, newCollectionId, Permission.Write) must_== Success(ContentCollRef(newCollectionId, Permission.Write.value, false))
    }

    "fail to add contentColRef if it exists already" in new TestScope {
      service.addCollection(orgId, collectionId, Permission.Read).isFailure must_== true
    }

    //TODO Do we really want to add two content coll refs with same collection but different permissions?
    //Ed: I think we should fail if this happens and add an updateCollection function -- or an updateCollection with an 'upsert' option a la mongo.
    "allow to add contentColRef with existing collection id but different perms" in new TestScope {
      service.addCollection(orgId, collectionId, Permission.Write) match {
        case Failure(e) => failure(s"Unexpected error $e")
        case Success(ccr) => success
      }
    }
  }

  "addCollectionReference" should {
    "add content coll reference to org" in new TestScope {
      val newCollectionId = ObjectId.get
      val ref = ContentCollRef(newCollectionId, Permission.Write.value)
      service.addCollectionReference(orgId, ref) match {
        case Failure(e) => failure(s"Unexpected error $e")
        case Success(_) => assertContentCollRefs(ref, newCollectionId, Permission.Write, orgId)
      }
    }
    "not fail when content coll ref is duplicate" in new TestScope {
      val ref = ContentCollRef(collectionId, Permission.Write.value)
      service.addCollectionReference(orgId, ref) match {
        case Failure(e) => failure(s"Unexpected error $e")
        case Success(_) => assertContentCollRefs(ref, collectionId, Permission.Write, orgId)
      }
    }
  }

  "addMetadataSet" should {
    "return the new ref" in new TestScope {
      service.addMetadataSet(orgId, setId) must equalTo(Success(MetadataSetRef(setId, true)))
    }
    "add a metadataset to the org" in new TestScope {
      service.addMetadataSet(orgId, setId)
      assertDbOrg(org.id) { _.metadataSets.length must_== 1 }
    }
  }

  "addPublicCollectionToAllOrgs" should {
    trait AddPublicCollectionScope extends TestScope {
      val testOrg = Organization("test owner of public collection")

      val publicCollection = ContentCollection("test public collection", testOrg.id, true)

      override def before = {
        super.before
        service.insert(testOrg, None)
      }

      override def after = {
        service.delete(testOrg.id)
        super.after
      }
    }
    "add a contentCollRef for the public collection to all orgs" in new AddPublicCollectionScope {
      service.addPublicCollectionToAllOrgs(publicCollection.id) match {
        case Failure(e) => failure(s"Unexpected error $e")
        case Success(_) => {
          val collRef = new ContentCollRef(publicCollection.id, Permission.Read.value, true)
          service.hasCollRef(testOrg.id, collRef) must_== true
          service.hasCollRef(org.id, collRef) must_== true
        }
      }
    }

    "give all orgs read access to a public collection" in new AddPublicCollectionScope {
      service.addPublicCollectionToAllOrgs(publicCollection.id) match {
        case Failure(e) => failure(s"Unexpected error $e")
        case Success(_) => {
          service.canAccessCollection(org.id, publicCollection.id, Permission.Read) must_== true
          service.canAccessCollection(testOrg.id, publicCollection.id, Permission.Read) must_== true
        }
      }
    }

    //TODO Any id can be added as public collection. Shouldn't we be a bit more strict?
    "succeed adding any id as public collection" in new AddPublicCollectionScope {
      val fakeId = ObjectId.get
      service.addPublicCollectionToAllOrgs(fakeId) match {
        case Failure(e) => failure(s"Unexpected error $e")
        case Success(_) => {
          val collRef = new ContentCollRef(fakeId, Permission.Read.value, true)
          service.hasCollRef(testOrg.id, collRef) must_== true
          service.hasCollRef(org.id, collRef) must_== true
          service.canAccessCollection(org.id, fakeId, Permission.Read) must_== true
          service.canAccessCollection(testOrg.id, fakeId, Permission.Read) must_== true
        }
      }
    }
  }

  "canAccessCollection" should {
    trait CanAccessCollectionScope extends TestScope {

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

      override def after = {
        services.contentCollectionService.delete(publicCollection.id)
        services.contentCollectionService.delete(testOrgWritableCollection.id)
        services.contentCollectionService.delete(ownWritableCollection.id)
        service.delete(testOrg.id)
        super.after
      }
    }
    "return true when accessing public collection with read permissions" in new CanAccessCollectionScope {
      service.canAccessCollection(org.id, publicCollection.id, Permission.Read) must_== true
    }
    "return false when accessing public collection with write permissions" in new CanAccessCollectionScope {
      service.canAccessCollection(org.id, publicCollection.id, Permission.Write) must_== false
    }
    "return true when accessing own readable collection with read permissions" in new CanAccessCollectionScope {
      service.canAccessCollection(org.id, collectionId, Permission.Read) must_== true
    }
    "return false when accessing own readable collection with write permissions" in new CanAccessCollectionScope {
      service.canAccessCollection(org.id, collectionId, Permission.Write) must_== false
    }
    "return true when accessing own writable collection with read permissions" in new CanAccessCollectionScope {
      service.canAccessCollection(org.id, ownWritableCollection.id, Permission.Read) must_== true
    }
    "return true when accessing own writable collection with write permissions" in new CanAccessCollectionScope {
      service.canAccessCollection(org.id, ownWritableCollection.id, Permission.Write) must_== true
    }
    "return false when accessing collection of other org with read permission" in new CanAccessCollectionScope {
      service.canAccessCollection(org.id, testOrgWritableCollection.id, Permission.Read) must_== false
    }
    "return false when accessing collection of other org with write permission" in new CanAccessCollectionScope {
      service.canAccessCollection(org.id, testOrgWritableCollection.id, Permission.Write) must_== false
    }
  }

  "changeName" should {

    "change the org name" in new TestScope {
      service.changeName(org.id, "update")
      assertDbOrg(org.id) { _.name must_== "update" }
    }

    "return the org id" in new TestScope {
      service.changeName(org.id, "update") match {
        case Success(orgId) => orgId must_== org.id
        case Failure(e) => failure(s"Unexpected error $e")
      }
    }

    "return an error if no org is found" in new TestScope {
      service.changeName(ObjectId.get, "update").swap.toOption.get must haveClass[GeneralError]
    }
  }

  "defaultCollection" should {
    "create new default collection, if it does not exist" in new TestScope {
      service.defaultCollection(org.id) match {
        case None => failure(s"Unexpected error")
        case Some(colId) => colId !== collectionId
      }
    }
    "return default collection, if it does exist" in new TestScope {
      val defaultCollection = new ContentCollection("default", org.id)
      services.contentCollectionService.insertCollection(org.id, defaultCollection, Permission.Write, enabled = true)

      service.defaultCollection(org.id) match {
        case None => failure(s"Unexpected error")
        case Some(colId) => colId must_== defaultCollection.id
      }
    }
    "fail if org does not exist" in new TestScope {
      service.defaultCollection(ObjectId.get) match {
        case None => success
        case Some(colId) => failure("Unexpected success")
      }
    }
  }

  "delete" should {
    trait DeleteScope extends TestScope {
      val childOrgOne = new Organization("child org one")
      val childOrgTwo = new Organization("child org two")

      override def before = {
        super.before
        service.insert(childOrgOne, Some(org.id))
        service.insert(childOrgTwo, Some(org.id))
      }

      override def after = {
        service.delete(childOrgOne.id)
        service.delete(childOrgTwo.id)
        super.after
      }

    }
    "remove the org itself" in new DeleteScope {
      orgExistsInDb(org.id) must_== true
      service.delete(org.id)
      orgExistsInDb(org.id) must_== false
    }
    "remove all the child orgs too" in new DeleteScope {
      orgExistsInDb(childOrgOne.id) must_== true
      orgExistsInDb(childOrgTwo.id) must_== true
      service.delete(org.id)
      orgExistsInDb(childOrgOne.id) must_== false
      orgExistsInDb(childOrgTwo.id) must_== false
    }
    "not fail if the org does not exist" in new DeleteScope {
      service.delete(ObjectId.get) match {
        case Failure(e) => failure(s"Unexpected error $e")
        case Success(e) => success
      }
    }
  }

  "deleteCollectionFromAllOrganizations" should {
    trait DeleteCollectionScope extends TestScope {
      val testOrg = new Organization("test org",
        contentcolls = Seq(ContentCollRef(collectionId = collectionId)))

      override def before = {
        super.before
        service.insert(testOrg, None)
      }

      override def after = {
        service.delete(testOrg.id)
        super.after
      }
    }

    "remove the collection from all orgs" in new DeleteCollectionScope {
      service.hasCollRef(org.id, contentCollRef) must_== true
      service.hasCollRef(testOrg.id, contentCollRef) must_== true

      service.deleteCollectionFromAllOrganizations(collectionId)

      service.hasCollRef(org.id, contentCollRef) must_== false
      service.hasCollRef(testOrg.id, contentCollRef) must_== false
    }
  }

  //TODO How are using enabled?
  "disableCollection" should {
    trait DisableCollectionScope extends TestScope {

      override def before: Unit = {
        super.before
        service.disableCollection(org.id, collectionId)
      }
    }

    "disable collection for org" in new DisableCollectionScope {
      service.disableCollection(org.id, collectionId) match {
        case Failure(e) => failure(s"Unexpected error with $e")
        case Success(r) => assertContentCollRefEnabled(r, false)
      }
    }

    "fail if org does not exist" in new DisableCollectionScope {
      service.disableCollection(ObjectId.get, collectionId) match {
        case Failure(e) => success
        case Success(r) => failure(s"Unexpected success with $r")
      }
    }

    "fail if collection does not exist" in new DisableCollectionScope {
      service.disableCollection(org.id, ObjectId.get) match {
        case Failure(e) => success
        case Success(r) => failure(s"Unexpected success with $r")
      }
    }
  }

  //TODO How are using enabled?
  "enableCollection" should {
    trait EnableCollectionScope extends TestScope {

      override def before: Unit = {
        super.before
        service.disableCollection(org.id, collectionId)
      }

    }

    "enable collection for org" in new EnableCollectionScope {
      service.enableCollection(org.id, collectionId) match {
        case Failure(e) => failure(s"Unexpected error with $e")
        case Success(r) => assertContentCollRefEnabled(r, true)
      }
    }

    "fail if org does not exist" in new EnableCollectionScope {
      service.enableCollection(ObjectId.get, collectionId) match {
        case Failure(e) => success
        case Success(r) => failure(s"Unexpected success with $r")
      }
    }

    "fail if collection does not exist" in new EnableCollectionScope {
      service.enableCollection(org.id, ObjectId.get) match {
        case Failure(e) => success
        case Success(r) => failure(s"Unexpected success with $r")
      }
    }
  }

  "findOneById" should {
    "return Some(org) if it can be found" in new TestScope {
      service.findOneById(org.id) match {
        case None => failure("Unexpected error")
        case Some(o) => o.id must_== org.id
      }
    }
    "return None if org does not exist" in new TestScope {
      service.findOneById(ObjectId.get) match {
        case None => success
        case Some(o) => failure("Unexpected success")
      }
    }
  }

  "findOneByName" should {
    //TODO Should insert check if a name is used already?
    "return first org, that hasbeen inserted with a name" in new TestScope {
      val org1 = Organization("X")
      val org2 = Organization("X")
      service.insert(org1, None)
      service.insert(org2, None)
      org1.id !== org2.id
      service.findOneByName("X") match {
        case None => failure("Unexpected error")
        case Some(o) => o.id must_== org1.id
      }
    }
    "return Some(org), if it can be found" in new TestScope {
      service.findOneByName(org.name) match {
        case None => failure("Unexpected error")
        case Some(o) => o.id must_== org.id
      }
    }
    "return None, if name is not in db" in new TestScope {
      service.findOneByName("non existent org name") match {
        case None => success
        case Some(o) => failure("Unexpected success")
      }
    }
  }

  "getDefaultCollection" should {
    "create new default collection, if it does not exist" in new TestScope {
      service.getDefaultCollection(org.id) match {
        case Failure(e) => failure(s"Unexpected error: $e")
        case Success(col) => col.id !== collectionId
      }
    }
    "return default collection, if it does exist" in new TestScope {
      val defaultCollection = new ContentCollection("default", org.id)
      services.contentCollectionService.insertCollection(org.id, defaultCollection, Permission.Write, enabled = true)

      service.getDefaultCollection(org.id) match {
        case Failure(e) => failure(s"Unexpected error: $e")
        case Success(col) => col.id must_== defaultCollection.id
      }
    }
    "fail if org does not exist" in new TestScope {
      service.getDefaultCollection(ObjectId.get) match {
        case Failure(e) => success
        case Success(col) => failure("Unexpected success")
      }
    }
  }

  "getOrgsWithAccessTo" should {
    trait GetOrgsScope extends TestScope {
      val newOrg = Organization("test org 2")
      service.insert(newOrg, None)
      service.addCollection(newOrg.id, collectionId, Permission.Read)

      override def after = {
        service.delete(newOrg.id)
        super.after
      }
    }
    "return all orgs with access to collection" in new GetOrgsScope {
      val orgs = service.getOrgsWithAccessTo(collectionId).toSeq
      assertSeqOfOrgs(orgs, org, newOrg)
    }

    "return empty seq if no org has access to collection" in new GetOrgsScope {
      val orgs = service.getOrgsWithAccessTo(ObjectId.get).toSeq
      orgs must_== Seq.empty
    }

  }

  "getTree" should {
    trait GetTreeScope extends TestScope {
      val childOne = Organization("childOne")
      val childTwo = Organization("childTwo")
      val grandChild = Organization("grandChild")

      override def before = {
        super.before
        service.insert(childOne, Some(org.id))
        service.insert(childTwo, Some(org.id))
        service.insert(grandChild, Some(childTwo.id))
      }

      override def after = {
        service.delete(childOne.id)
        service.delete(childTwo.id)
        service.delete(grandChild.id)
        super.after
      }
    }

    "return empty seq when org does not exist" in new GetTreeScope {
      service.getTree(ObjectId.get) must_== Seq.empty
    }
    "return just org when it does not have any children" in new GetTreeScope {
      assertSeqOfOrgs(service.getTree(childOne.id), childOne)
    }
    "return org and and child" in new GetTreeScope {
      assertSeqOfOrgs(service.getTree(childTwo.id), childTwo, grandChild)
    }
    "return org, child and grand child" in new GetTreeScope {
      assertSeqOfOrgs(service.getTree(org.id), org, childOne, childTwo, grandChild)
    }
  }

  "hasCollRef" should {
    "return true when collectionId and permissions are correct" in new TestScope {
      service.hasCollRef(org.id, ContentCollRef(collectionId, Permission.Read.value)) must_== true
    }
    "return false when collectionId is not correct" in new TestScope {
      service.hasCollRef(org.id, ContentCollRef(ObjectId.get, Permission.Read.value)) must_== false
    }
    "return false when permission is not correct" in new TestScope {
      service.hasCollRef(org.id, ContentCollRef(collectionId, Permission.Write.value)) must_== false
    }
  }

  "insert" should {
    trait InsertScope extends TestScope {

      val childOrg = Organization("child")

      override def before = {
        super.before
        service.insert(childOrg, Some(org.id))
      }
    }
    "add the org to the db" in new InsertScope {
      assertDbOrg(org.id) { _.id must_== org.id }
    }
    "add org's own id to the paths if parent is None" in new InsertScope {
      assertDbOrg(org.id) { _.path must_== Seq(org.id) }
    }
    "add the parent's id to the paths if parent is not None" in new InsertScope {
      assertDbOrg(childOrg.id) { _.path must_== Seq(childOrg.id, org.id) }
    }
  }

  "isChild" should {
    trait IsChildScope extends TestScope {
      val childOrg = Organization("child")

      override def before = {
        super.before
        service.insert(childOrg, Some(org.id))
      }
    }

    "return true, when child has been inserted with parent" in new IsChildScope {
      service.isChild(org.id, childOrg.id) must_== true
    }
    "return false, when child does not exist" in new IsChildScope {
      service.isChild(org.id, ObjectId.get) must_== false
    }
    "return false, when parent does not exist" in new IsChildScope {
      service.isChild(ObjectId.get, childOrg.id) must_== false
    }
  }

  "orgsWithPath" should {

    trait OrgsWithPathScope extends TestScope {
      val childOrg = Organization("child")
      val grandChildOrg = Organization("grand child")

      override def before = {
        super.before
        service.insert(childOrg, Some(org.id))
        service.insert(grandChildOrg, Some(childOrg.id))
      }
    }

    "return parent only, when deep = false" in new OrgsWithPathScope {
      assertSeqOfOrgs(service.orgsWithPath(org.id, deep = false), org)
    }
    "return parent and child, when deep = true" in new OrgsWithPathScope {
      assertSeqOfOrgs(service.orgsWithPath(childOrg.id, deep = true), childOrg, grandChildOrg)
    }
    "also returns deeply nested orgs (> one level), when deep = true" in new OrgsWithPathScope {
      assertSeqOfOrgs(service.orgsWithPath(org.id, deep = true), org, childOrg, grandChildOrg)
    }
  }

  "removeCollection" should {
    "remove collection from org" in new TestScope {
      service.canAccessCollection(org.id, collectionId, Permission.Read) must_== true
      service.removeCollection(org.id, collectionId) match {
        case Success(u) => success
        case Failure(e) => failure(s"Unexpected error $e")
      }
      service.canAccessCollection(org.id, collectionId, Permission.Read) must_== false
    }
    "fail when org does not exist" in new TestScope {
      service.removeCollection(ObjectId.get, collectionId) match {
        case Success(_) => failure(s"Unexpected success")
        case Failure(e) => e must haveClass[GeneralError]
      }
    }
    "not fail, when collection does not exist" in new TestScope {
      service.removeCollection(org.id, ObjectId.get) match {
        case Success(_) => success
        case Failure(e) => failure(s"Unexpected error $e")
      }
    }
    //TODO Do we really want to be able to remove a public collection from an org?
    //TODO That seems to be inconsistent with hasAccessToCollection
    "allow to remove a public collection" in new TestScope {
      val publicOrg = Organization("Public")
      val publicCollection = ContentCollection("public", publicOrg.id, isPublic = true)
      services.contentCollectionService.insertCollection(publicOrg.id, publicCollection, Permission.Write, true)
      service.addPublicCollectionToAllOrgs(publicCollection.id)

      service.hasCollRef(orgId, ContentCollRef(publicCollection.id)) must_== true
      service.removeCollection(org.id, publicCollection.id)
      service.hasCollRef(orgId, ContentCollRef(publicCollection.id)) must_== false
    }
  }

  "removeMetadataSet" should {

    "remove a metadataset" in new TestScope {
      service.addMetadataSet(orgId, setId)
      assertDbOrg(org.id) { _.metadataSets.length must_== 1 }
      service.removeMetadataSet(orgId, setId)
      assertDbOrg(org.id) { _.metadataSets.length must_== 0 }
    }

    "return the metadataset, which has been removed" in new TestScope {
      service.addMetadataSet(orgId, setId)
      service.removeMetadataSet(orgId, setId) match {
        case Success(mds) => mds.metadataId must_== setId
        case Failure(e) => failure(s"Unexpected error $e")
      }
    }

    "fail when org cannot be found" in new TestScope {
      service.removeMetadataSet(ObjectId.get, setId) match {
        case Success(_) => failure("Unexpected success")
        case Failure(e) => e must haveClass[GeneralError]
      }
    }

    "fail when metadata set cannot be found" in new TestScope {
      service.removeMetadataSet(org.id, ObjectId.get) match {
        case Success(_) => failure("Unexpected success")
        case Failure(e) => e must haveClass[GeneralError]
      }
    }
  }

  "updateCollection" should {
    //TODO We cannot update the perms?
    "not allow to change the permissions" in new TestScope {
      service.canAccessCollection(org.id, collectionId, Permission.Write) must_== false
      service.updateCollection(orgId, ContentCollRef(collectionId, Permission.Write.value)) match {
        case Success(_) => failure("Unexpected success")
        case Failure(e) => e must haveClass[GeneralError]
      }
      service.canAccessCollection(org.id, collectionId, Permission.Write) must_== false
    }

    //TODO How are we using enabled?
    "allow to change enabled" in new TestScope {
      assertDbOrg(org.id) { _.contentcolls(0).enabled must_== true }

      service.updateCollection(orgId, ContentCollRef(collectionId, Permission.Read.value, enabled = false)) match {
        case Success(_) => success
        case Failure(e) => failure(s"Unexpected failure: $e")
      }

      assertDbOrg(org.id) { _.contentcolls(0).enabled must_== false }
    }

    "fail when org does not exist" in new TestScope {
      service.updateCollection(ObjectId.get, contentCollRef) match {
        case Success(_) => failure("Unexpected success")
        case Failure(e) => e must haveClass[GeneralError]
      }
    }
    "fail when collection does not exist in org" in new TestScope {
      service.updateCollection(org.id, ContentCollRef(ObjectId.get, Permission.Write.value)) match {
        case Success(_) => failure("Unexpected success")
        case Failure(e) => e must haveClass[GeneralError]
      }
    }
  }

  "updateOrganization" should {
    "update the org name in the db" in new TestScope {
      val update = org.copy(name = "update")
      service.updateOrganization(update)
      assertDbOrg(update.id) { _.name must_== "update" }
    }
    "return the updated org object" in new TestScope {
      val update = org.copy(name = "update")
      service.updateOrganization(update) match {
        case Success(o) => o.name must_== "update"
        case Failure(e) => failure(s"Unexpected error $e")
      }
    }
    "not change the contentcolls" in new TestScope {
      val updatedContentcolls = Seq(ContentCollRef(ObjectId.get))
      val update = org.copy(contentcolls = updatedContentcolls)
      service.updateOrganization(update)
      assertDbOrg(update.id) { _.contentcolls !== updatedContentcolls }
    }
    "not change the metadatasets" in new TestScope {
      val updatedMetadataSets = Seq(MetadataSetRef(ObjectId.get, false))
      val update = org.copy(metadataSets = updatedMetadataSets)
      service.updateOrganization(update)
      assertDbOrg(update.id) { _.contentcolls !== updatedMetadataSets }
    }
    "fail when org cannot be found" in new TestScope {
      val update = org.copy(id = ObjectId.get, name = "update")
      service.updateOrganization(update) match {
        case Success(_) => failure("Unexpected success")
        case Failure(e) => e must haveClass[GeneralError]
      }
    }
  }

}

