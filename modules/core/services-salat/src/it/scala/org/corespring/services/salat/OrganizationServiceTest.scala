package org.corespring.services.salat

import org.bson.types.ObjectId
import org.corespring.errors.{ GeneralError, PlatformServiceError }
import org.corespring.models.auth.Permission
import org.corespring.models.{ ContentCollRef, MetadataSetRef, Organization }
import org.specs2.mock.Mockito
import org.specs2.mutable.After

import scalaz.{ Failure, Success }

class OrganizationServiceTest extends ServicesSalatIntegrationTest with Mockito {

  trait scope extends After {

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

