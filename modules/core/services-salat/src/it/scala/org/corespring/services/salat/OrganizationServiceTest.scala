package org.corespring.services.salat

import org.bson.types.ObjectId
import org.corespring.models.auth.Permission
import org.corespring.models.metadata.MetadataSet
import org.corespring.models.{ ContentCollection, MetadataSetRef, ContentCollRef, Organization }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.errors.{ PlatformServiceError, GeneralError }
import org.specs2.mock.Mockito
import org.specs2.mutable.BeforeAfter

import scalaz.{ Validation, Failure, Success }

class OrganizationServiceTest extends ServicesSalatIntegrationTest with Mockito {

  trait TestScope extends BeforeAfter {

    lazy val service = services.orgService
    lazy val orgId: ObjectId = ObjectId.get
    lazy val collectionId: ObjectId = ObjectId.get
    lazy val contentCollRef = ContentCollRef(collectionId = collectionId, Permission.Read.value, enabled=true)
    lazy val org = Organization(
      name = "orgservice-test-org",
      id = orgId,
      contentcolls = Seq(contentCollRef))
    lazy val setId: ObjectId = ObjectId.get

    def after: Any = service.delete(orgId)

    def before: Any = service.insert(org, None)

    def assertCCR(ccr: ContentCollRef, collId: ObjectId, p: Permission, org: ObjectId) = {
      ccr.collectionId === collId
      ccr.pval === p.value
      service.hasCollRef(orgId, ccr) === true
    }

    def assertSeqOfOrgs(orgs: Seq[Organization], orgsToFind: Organization*) = {
      for( orgToFind <- orgsToFind) {
        if(orgs.find(o => o.id == orgToFind.id) == None){
          failure(s"Expected to find ${orgToFind.name} in seq")
        }
      }
      orgsToFind.length === orgs.length
    }

    def assertEnabled(ref : ContentCollRef, expectedStatus: Boolean) = {
      ref.enabled === expectedStatus
      service.findOneById(org.id).get
        .contentcolls
        .find(_.collectionId == collectionId)
        .map(_.enabled) === Some(expectedStatus)
    }
  }

  "addCollection" should {

    "add new collection id with read permission" in new TestScope {
      val newCollectionId = ObjectId.get
      service.addCollection(orgId, newCollectionId, Permission.Read) match {
        case Failure(e) => failure(s"Unexpected error $e")
        case Success(ccr) => assertCCR(ccr, newCollectionId, Permission.Read, orgId)
      }
    }

    "add new collection id with write permission" in new TestScope {
      val newCollectionId = ObjectId.get
      service.addCollection(orgId, newCollectionId, Permission.Write) match {
        case Failure(e) => failure(s"Unexpected error $e")
        case Success(ccr) => assertCCR(ccr, newCollectionId, Permission.Write, orgId)
      }
    }

    "fail to add collection id, if it does exists in content coll refs" in new TestScope {
      service.addCollection(orgId, collectionId, Permission.Read) match {
        case Failure(e) => success
        case Success(ccr) => failure(s"Unexpected success")
      }
    }
  }

  "addCollectionReference" should {
    "add content coll reference to org" in new TestScope {
      val newCollectionId = ObjectId.get
      val ref = ContentCollRef(newCollectionId, Permission.Write.value)
      service.addCollectionReference(orgId, ref) match {
        case Failure(e) => failure(s"Unexpected error $e")
        case Success(_) => assertCCR(ref, newCollectionId, Permission.Write, orgId)
      }
    }
    "not fail when content coll ref is duplicate" in new TestScope {
      val ref = ContentCollRef(collectionId, Permission.Write.value)
      service.addCollectionReference(orgId, ref) match {
        case Failure(e) => failure(s"Unexpected error $e")
        case Success(_) => assertCCR(ref, collectionId, Permission.Write, orgId)
      }
    }
  }

  "addMetadataSet" should {
    "if the set does not exist" should {
      "return an error when checkExistence is the default" in new TestScope {
        service.addMetadataSet(orgId, setId) must equalTo(Failure("couldn't find the metadata set"))
      }
      "return an error when checkExistence = true" in new TestScope {
        service.addMetadataSet(orgId, setId, true) must equalTo(Failure("couldn't find the metadata set"))
      }
      "if checkExistence is false" should {
        "return the new ref" in new TestScope {
          service.addMetadataSet(orgId, setId, false) must equalTo(Success(MetadataSetRef(setId, true)))
        }
        "add a metadataset to the org" in new TestScope {
          service.addMetadataSet(orgId, setId, false)
          service.findOneById(org.id).map {
            org => org.metadataSets.length === 1
          }.getOrElse(failure("didn't find org"))
        }
      }
    }
    "if the set exists" should {
      trait WithSetScope extends TestScope {
        val metadataSet = new MetadataSet("", "", "")
        //TODO Can we insert a metadata set only? Update is not the correct method
        services.metadataSetService.update(metadataSet)

        override def after = {
          //TODO Can we remove a metadata set only? Delete also tries to update the org
          services.metadataSetService.delete(ObjectId.get, metadataSet.id)
          super.after
        }
      }
      "return no error when checkExistence is default" in new WithSetScope {
        service.addMetadataSet(orgId, metadataSet.id).isSuccess === true
      }
      "return no error when checkExistence is true" in new WithSetScope {
        service.addMetadataSet(orgId, metadataSet.id, true).isSuccess === true
      }
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
    "add the collection to all orgs" in new AddPublicCollectionScope {
      service.addPublicCollectionToAllOrgs(publicCollection.id) match {
        case Failure(e) => failure(s"Unexpected error $e")
        case Success(_) => {
          val collRef = new ContentCollRef(publicCollection.id, Permission.Read.value, true)
          service.hasCollRef(testOrg.id, collRef) === true
          service.hasCollRef(org.id, collRef) === true
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
      service.canAccessCollection(org.id, publicCollection.id, Permission.Read) === true
    }
    "return false when accessing public collection with write permissions" in new CanAccessCollectionScope {
      service.canAccessCollection(org.id, publicCollection.id, Permission.Write) === false
    }
    "return true when accessing own readable collection with read permissions" in new CanAccessCollectionScope {
      service.canAccessCollection(org.id, collectionId, Permission.Read) === true
    }
    "return false when accessing own readable collection with write permissions" in new CanAccessCollectionScope {
      service.canAccessCollection(org.id, collectionId, Permission.Write) === false
    }
    "return true when accessing own writable collection with read permissions" in new CanAccessCollectionScope {
      service.canAccessCollection(org.id, ownWritableCollection.id, Permission.Read) === true
    }
    "return true when accessing own writable collection with write permissions" in new CanAccessCollectionScope {
      service.canAccessCollection(org.id, ownWritableCollection.id, Permission.Write) === true
    }
    "return false when accessing collection of other org with read permission" in new CanAccessCollectionScope {
      service.canAccessCollection(org.id, testOrgWritableCollection.id, Permission.Read) === false
    }
    "return false when accessing collection of other org with write permission" in new CanAccessCollectionScope {
      service.canAccessCollection(org.id, testOrgWritableCollection.id, Permission.Write) === false
    }
  }

  "changeName" should {

    "change the org name" in new TestScope {
      service.changeName(org.id, "update")
      service.findOneById(org.id).map(_.name) === Some("update")
    }

    "return an error if no org is found" in new TestScope {
      service.changeName(ObjectId.get, "update").swap.toOption.get must haveClass[GeneralError]
    }
  }

  "defaultCollection" should {
    //deprecated, see getDefaultCollection
    "work" in pending
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
      service.findOneById(org.id) !== None
      service.delete(org.id)
      service.findOneById(org.id) === None
    }
    "remove all the child orgs too" in new DeleteScope {
      service.findOneById(childOrgOne.id) !== None
      service.findOneById(childOrgTwo.id) !== None
      service.delete(org.id)
      service.findOneById(childOrgOne.id) === None
      service.findOneById(childOrgTwo.id) === None
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
      service.canAccessCollection(org, collectionId, Permission.Read) === true
      service.canAccessCollection(testOrg, collectionId, Permission.Read) === true

      service.deleteCollectionFromAllOrganizations(collectionId)

      service.canAccessCollection(org, collectionId, Permission.Read) === false
      service.canAccessCollection(testOrg, collectionId, Permission.Read) === false
    }
  }

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
        case Success(r) => r.enabled === false
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
        case Success(r) => assertEnabled(r, true)
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
    "find org" in new TestScope {
      service.findOneById(org.id) match {
        case None => failure("Unexpected error")
        case Some(o) => o.id === org.id
      }
    }
  }

  "findOneByName" should {
    //TODO insert does not check if a name is used already
    "find org" in new TestScope {
      service.findOneByName(org.name) match {
        case None => failure("Unexpected error")
        case Some(o) => o.id === org.id
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
        case Success(col) => col.id === defaultCollection.id
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
    "return org" in new TestScope {
      service.getOrgsWithAccessTo(collectionId).map(o => o.id === org.id)
    }
    "return all orgs" in new GetOrgsScope {
      val orgs = service.getOrgsWithAccessTo(collectionId).toSeq
      assertSeqOfOrgs(orgs, org, newOrg)
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
      service.getTree(ObjectId.get) === Seq.empty
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
      service.hasCollRef(org.id, ContentCollRef(collectionId, Permission.Read.value)) === true
    }
    "return false when collectionId is not correct" in new TestScope {
      service.hasCollRef(org.id, ContentCollRef(ObjectId.get, Permission.Read.value)) === false
    }
    "return false when permission is not correct" in new TestScope {
      service.hasCollRef(org.id, ContentCollRef(collectionId, Permission.Write.value)) === false
    }
  }

  "insert" should {
    "add the org to the db" in new TestScope {
      service.findOneById(org.id).get.id === org.id
    }
    "add its own id to the paths if parent is None" in new TestScope {
      service.findOneById(org.id).get.path === Seq(org.id)
    }
    "add the parent's id to the paths if parent is not None" in new TestScope {
      val childOrg = Organization("child")
      service.insert(childOrg, Some(org.id))
      service.findOneById(childOrg.id).get.path === Seq(childOrg.id, org.id)
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
      service.isChild(org.id, childOrg.id) === true
    }
    "return false, when child does not exist" in new IsChildScope {
      service.isChild(org.id, ObjectId.get) === false
    }
    "return false, when parent does not exist" in new IsChildScope {
      service.isChild(ObjectId.get, childOrg.id) === false
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
      assertSeqOfOrgs(service.orgsWithPath(org.id, deep=false), org)
    }
    "return parent and child when deep = true" in new OrgsWithPathScope {
      assertSeqOfOrgs(service.orgsWithPath(childOrg.id, deep=true), childOrg, grandChildOrg)
    }
    "return parent, child and grand child, when deep = true" in new OrgsWithPathScope {
      assertSeqOfOrgs(service.orgsWithPath(org.id, deep=true), org, childOrg, grandChildOrg)
    }
  }

  "removeCollection" should {
    "remove collection from org" in new TestScope {
      service.canAccessCollection(org.id, collectionId, Permission.Read) === true
      service.removeCollection(org.id, collectionId) match {
        case Success(u) => success
        case Failure(e) => failure(s"Unexpected error $e")
      }
      service.canAccessCollection(org.id, collectionId, Permission.Read) === false
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

      service.hasCollRef(orgId, ContentCollRef(publicCollection.id)) === true
      service.removeCollection(org.id, publicCollection.id)
      service.hasCollRef(orgId, ContentCollRef(publicCollection.id)) === false
    }
  }

  "removeMetadataSet" should {

    "remove a metadataset" in new TestScope {
      service.addMetadataSet(orgId, setId, false)
      service.findOneById(org.id).map { org => org.metadataSets.length === 1 }.getOrElse(failure("didn't find org"))
      service.removeMetadataSet(orgId, setId)
      service.findOneById(org.id).map { org => org.metadataSets.length === 0 }.getOrElse(failure("didn't find org"))
    }

    "return the metadataset, which has been removed" in new TestScope {
      service.addMetadataSet(orgId, setId, false)
      service.removeMetadataSet(orgId, setId) match {
        case Success(mds) => mds.metadataId === setId
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
      service.canAccessCollection(org.id, collectionId, Permission.Write) === false
      service.updateCollection(orgId, ContentCollRef(collectionId, Permission.Write.value)) match {
        case Success(_) => failure("Unexpected success")
        case Failure(e) => e must haveClass[GeneralError]
      }
      service.canAccessCollection(org.id, collectionId, Permission.Write) === false
    }

    //TODO How are we using enabled?
    "allow to change enabled" in new TestScope {
      service.findOneById(org.id).map { org => org.contentcolls(0).enabled === true }.getOrElse(failure("didn't find org"))

      service.updateCollection(orgId, ContentCollRef(collectionId, Permission.Read.value, enabled=false)) match {
        case Success(_) => success
        case Failure(e) => failure(s"Unexpected failure: $e")
      }
      service.findOneById(org.id).map { org => org.contentcolls(0).enabled === false }.getOrElse(failure("didn't find org"))
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
    "update the org in the db" in new TestScope {
      val update = org.copy(name = "update")
      service.updateOrganization(update)
      service.findOneById(update.id).map(_.name) === Some("update")
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

