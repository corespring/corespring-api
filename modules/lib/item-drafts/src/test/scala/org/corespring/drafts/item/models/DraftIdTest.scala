package org.corespring.drafts.item.models

import org.bson.types.ObjectId
import org.specs2.mutable.Specification

class DraftIdTest extends Specification {

  val itemId = ObjectId.get
  val orgId = ObjectId.get

  "toIdString" should {

    "create a string in the form id~name" in {
      new DraftId(itemId, "name", orgId).toIdString must_== s"$itemId~name"
    }

    "url encode the name" in {
      new DraftId(itemId, "name how are you", orgId).toIdString must_== s"$itemId~name+how+are+you"
    }
  }

  "fromIdString" should {

    "read an id string" in {
      DraftId.fromIdString(s"$itemId~name", orgId) must_== Some(DraftId(itemId, "name", orgId))
    }

    "read a versioned id string" in {
      DraftId.fromIdString(s"$itemId:0~name", orgId) must_== Some(DraftId(itemId, "name", orgId))
    }

    "url decode the name id string" in {
      DraftId.fromIdString(s"$itemId~name+how+are+you", orgId) must_== Some(DraftId(itemId, "name how are you", orgId))
    }
  }
}
