package org.corespring.drafts.item

import org.bson.types.ObjectId
import org.corespring.drafts.item.models.{ SimpleUser, SimpleOrg, OrgAndUser }
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class MakeDraftIdTest extends Specification {

  trait scope extends Scope with MakeDraftId {
    val simpleOrg = SimpleOrg(ObjectId.get, "org")
    val simpleUser = SimpleUser(ObjectId.get, "default-user", "pass", "default user", simpleOrg.id)
  }

  "MakeDraftIdTest" should {
    "mkDraftId" in new scope {
      val ou = OrgAndUser(simpleOrg, None)
      val draftId = mkDraftId(ou, s"${ObjectId.get.toString}~hi+there").toOption.get
      draftId.name must_== "hi there"
    }
  }
}
