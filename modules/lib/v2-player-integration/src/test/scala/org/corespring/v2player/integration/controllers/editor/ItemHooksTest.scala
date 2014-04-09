package org.corespring.v2player.integration.controllers.editor

import java.util.concurrent.TimeUnit
import org.bson.types.ObjectId
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.UserService
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.core.services.organization.OrganizationService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2player.integration.securesocial.SecureSocialService
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.{ Json, JsValue }
import play.api.mvc.SimpleResult
import play.api.test.FakeRequest
import scala.concurrent.duration.Duration
import scala.concurrent.{ Future, Await, ExecutionContext }
import play.api.test.Helpers._
import play.api.mvc.Results._

class ItemHooksTest extends Specification with Mockito {

  import ExecutionContext.Implicits.global

  def hooks(item: Option[Item] = None) = new ItemHooks {
    override def orgService: OrganizationService = mock[OrganizationService]

    override def userService: UserService = mock[UserService]

    override def secureSocialService: SecureSocialService = mock[SecureSocialService]

    override implicit def executionContext: ExecutionContext = ExecutionContext.Implicits.global

    override def transform: (Item) => JsValue = (i: Item) => Json.obj()

    override def itemService: ItemService = {
      val m = mock[ItemService]
      m.findOneById(any[VersionedId[ObjectId]]) returns item
      m
    }
  }

  class loadContext(item: Option[Item] = None) extends Scope {
    lazy val f = hooks(item).load(s"${ObjectId.get}")(FakeRequest("", ""))
    lazy val futureResult: Future[SimpleResult] = f.map { either =>
      either match {
        case Left(r) => r
        case Right(json) => Ok(json)
      }
    }
    lazy val result: Either[SimpleResult, JsValue] = Await.result(f, Duration(1, TimeUnit.SECONDS))
  }

  "load" should {

    "return not found for no item" in new loadContext() {
      status(futureResult) === NOT_FOUND
    }

    "return ok for item" in new loadContext(Some(Item())) {
      status(futureResult) === OK
    }
  }
}
