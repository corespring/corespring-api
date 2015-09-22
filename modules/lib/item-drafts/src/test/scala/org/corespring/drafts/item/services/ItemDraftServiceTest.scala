package org.corespring.drafts.item.services

import com.mongodb.casbah.Imports
import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId
import org.corespring.drafts.item.models._
import org.corespring.test.PlaySingleton
import org.corespring.test.fakes.Fakes.withMockCollection
import org.joda.time.DateTime
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import org.corespring.platform.core.models.item.Item

class ItemDraftServiceTest extends Specification {

  PlaySingleton.start()

  private trait scope extends Scope with withMockCollection {

    val orgId = ObjectId.get
    val itemId = ObjectId.get

    val draftId = DraftId(itemId, "name", orgId)

    def mkUser(userName: String) = SimpleUser(ObjectId.get, userName, "full name", "provider", orgId)

    val itemDraft = ItemDraft(
      draftId,
      Item(),
      OrgAndUser(
        SimpleOrg(orgId, "org-name"),
        None))

    val service = new ItemDraftService {
      override def collection: Imports.MongoCollection = mockCollection
    }
  }

  "listForOrg" should {

    "query by orgId" in new scope {
      service.listForOrg(orgId)
      val (q, _) = captureFind
      q.value must_== MongoDBObject("_id.orgId" -> orgId)
    }

    "request a limited field set" in new scope {
      service.listForOrg(orgId)
      val (_, f) = captureFind
      f.value must_== MongoDBObject("created" -> 1, "expires" -> 1, "user.user.userName" -> 1)
    }

    "return ItemDraftHeaders" in new scope {

      val idDbo = MongoDBObject(
        "itemId" -> itemId,
        "orgId" -> orgId,
        "name" -> "name")

      val created = DateTime.now
      val expires = created.plusHours(24)

      override lazy val findResultSeq = Seq(MongoDBObject(
        "_id" -> idDbo,
        "created" -> created.toDate,
        "expires" -> expires.toDate,
        "user" -> MongoDBObject("user" -> MongoDBObject("userName" -> "name"))))

      val result = service.listForOrg(orgId)

      val firstItem = result.headOption
      firstItem.map(_.id.itemId) must_== Some(itemId)
      firstItem.map(_.id.orgId) must_== Some(orgId)
      firstItem.map(_.created) === Some(created)
      firstItem.map(_.expires) === Some(expires)
      firstItem.map(_.userName).flatten === Some("name")
    }
  }

  "load" should {
    "call collection.findOne" in new scope {
      service.load(draftId)
      val q = captureFindOneQueryOnly
      q.value === ItemDraftDbUtils.idToDbo(draftId)
    }

    "return a draft" in new scope {
      override lazy val findOneResult = ItemDraftDbUtils.toDbo(itemDraft)
      service.load(draftId) === Some(itemDraft)
    }
  }

  "owns" should {
    "return true if orgId matches draft.id.orgId" in new scope {
      service.owns(OrgAndUser(SimpleOrg(orgId, "org-name"), None), DraftId(itemId, "name", orgId)) === true
    }

    "return false if orgId does not draft.id.orgId" in new scope {
      service.owns(OrgAndUser(SimpleOrg(ObjectId.get, "org-name"), None), DraftId(itemId, "name", orgId)) === false
    }

    "return true if user does match" in new scope {
      service.owns(
        OrgAndUser(
          SimpleOrg(ObjectId.get, "org-name"),
          Some(mkUser("userName"))),
        DraftId(itemId, "userName", orgId)) === false
    }

    "return false if user does not match" in new scope {
      service.owns(
        OrgAndUser(
          SimpleOrg(ObjectId.get, "org-name"),
          Some(mkUser("userName"))),
        DraftId(itemId, "otherUserName", orgId)) === false
    }
  }

  "remove" should {
    "call collection.remove" in new scope {
      service.remove(draftId)
      val r = captureRemoveQueryOnly
      r.value === ItemDraftDbUtils.idToDbo(draftId)
    }
  }
}
