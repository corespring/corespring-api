package org.corespring.v2.api.drafts.item

import org.bson.types.ObjectId
import org.corespring.drafts.item
import org.corespring.drafts.item.models.{ SimpleOrg, OrgAndUser }
import org.corespring.platform.data.mongo.models.VersionedId
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.mvc.RequestHeader
import play.api.test.{ PlaySpecification, FakeRequest }

class ItemDraftsTest extends Specification with PlaySpecification with Mockito {

  trait TestController extends ItemDrafts {
    override def drafts: item.ItemDrafts = ???

    override def identifyUser(rh: RequestHeader): Option[OrgAndUser] = ???
  }

  "ItemDrafts" should {

    val req = FakeRequest("", "")

    "list" should {

      val user = OrgAndUser(SimpleOrg(ObjectId.get, "test-org"), None)

      class scp(user: Option[OrgAndUser] = None) extends Scope {
        val itemId = VersionedId(ObjectId.get, Some(0))
        val controller = new TestController {
          override def identifyUser(rh: RequestHeader) = user

          override def drafts: item.ItemDrafts = mock[item.ItemDrafts].list(itemId) returns Seq.empty
        }
      }

      "return error" in new scp {
        val result = controller.list(itemId.toString)(req)
        status(result) === UNAUTHORIZED
      }

      "return error if id is bad" in new scp(Some(user)) {
        val result = controller.list("?")(req)
        status(result) === BAD_REQUEST
      }

      "return a list" in new scp(Some(user)) {
        val result = controller.list(itemId.toString)(req)
        status(result) === OK
      }
    }

    "create" should {
      "work" in { true === false }.pendingUntilFixed
    }

    "commit" should {
      "work" in { true === false }.pendingUntilFixed
    }

    "get" should {
      "work" in { true === false }.pendingUntilFixed
    }

    "save" should {
      "work" in { true === false }.pendingUntilFixed
    }

    "delete" should {
      "work" in { true === false }.pendingUntilFixed
    }

  }
}
