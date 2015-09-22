package org.corespring.drafts.item.services

import com.mongodb.casbah.Imports
import com.mongodb.casbah.commons.MongoDBObject
import org.bson.types.ObjectId
import org.corespring.test.PlaySingleton
import org.corespring.test.fakes.Fakes.withMockCollection
import org.joda.time.DateTime
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class ItemDraftServiceTest extends Specification {

  PlaySingleton.start()

  trait scope extends Scope with withMockCollection {

    val orgId = ObjectId.get
    val itemId = ObjectId.get

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
}
