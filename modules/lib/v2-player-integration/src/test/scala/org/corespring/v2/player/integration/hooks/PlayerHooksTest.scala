package org.corespring.v2.player.hooks

import java.util.concurrent.TimeUnit

import org.bson.types.ObjectId
import org.corespring.platform.core.models.item.PlayerDefinition
import org.corespring.platform.core.models.Organization
import org.corespring.platform.core.models.item.{ Item, PlayerDefinition }
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.qtiToV2.transformers.ItemTransformer
import org.corespring.test.PlaySingleton
import org.corespring.v2.auth.SessionAuth
import org.corespring.v2.auth.models.{ MockFactory, AuthMode, OrgAndOpts, PlayerAccessSettings }
import org.corespring.v2.errors.Errors.cantLoadSession
import org.corespring.v2.errors.V2Error
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc._
import play.api.test.{ WithApplication, FakeRequest }

import scalaz.{ Failure, Success, Validation }

class PlayerHooksTest extends Specification with Mockito with MockFactory {

  lazy val orgAndOpts = OrgAndOpts(mockOrg(), PlayerAccessSettings.ANYTHING, AuthMode.AccessToken, None)

  PlaySingleton.start()

  class defaultScope(orgAndOptsResult: Validation[V2Error, OrgAndOpts] = Success(orgAndOpts))
    extends Scope
    with PlayerHooks {

    val mockItemService = mock[ItemService]
    val mockItemTransformer = {
      val m = mock[ItemTransformer]
      m.loadItemAndUpdateV2(any[VersionedId[ObjectId]]) answers { (vid) =>
        Some(Item(id = vid.asInstanceOf[VersionedId[ObjectId]], playerDefinition = Some(PlayerDefinition("hi"))))
      }
      m
    }
    val mockAuth = {
      val m = mock[SessionAuth[OrgAndOpts, PlayerDefinition]]
      m.canCreate(any[String])(any[OrgAndOpts]) returns Success(true)
      m.create(any[JsValue])(any[OrgAndOpts]) answers { (obj, m) =>
        Success(new ObjectId())
      }
      m
    }

    override def itemService: ItemService = mockItemService

    override def itemTransformer: ItemTransformer = mockItemTransformer

    override def auth: SessionAuth[OrgAndOpts, PlayerDefinition] = mockAuth

    override def loadFile(id: String, path: String)(request: Request[AnyContent]): SimpleResult = Results.Ok("")

    override def loadItemFile(itemId: String, file: String)(implicit header: RequestHeader): SimpleResult = Results.Ok("")

    override def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = orgAndOptsResult

  }

  class hooksScope(orgAndOptsResult: Validation[V2Error, OrgAndOpts] = Success(orgAndOpts),
    loadForReadResult: Validation[V2Error, (JsValue, PlayerDefinition)] = Success(Json.obj() -> PlayerDefinition(Seq.empty, "", Json.obj(), "", None)))
    extends defaultScope(orgAndOptsResult) {

    mockAuth.loadForRead(any[String])(any[OrgAndOpts]) returns loadForReadResult
  }

  "PlayerHooks" should {

    import scala.concurrent._
    import scala.concurrent.duration._

    val cantLoadSessionError = cantLoadSession("bad session")

    "loadSessionAndItem" should {
      "pass back the status code" in new hooksScope(loadForReadResult = Failure(cantLoadSessionError)) {
        val future = loadSessionAndItem("sessionId")(FakeRequest("", ""))
        val either = Await.result(future, Duration(1, TimeUnit.SECONDS))
        either === Left(cantLoadSessionError.statusCode -> cantLoadSessionError.message)
      }
    }

    "createSessionForItem" should {

      class createSessionScope extends defaultScope {}

      "fail if it can't find org and opts" in pending
      "fail if can create fails" in pending
      "fail if it's an invalid versioned id" in pending
      "fail if itemTransformer fails to load the item" in pending
      "fail if create session fails" in pending

      "return the session and item" in new createSessionScope() {
        val versionedId = s"${ObjectId.get.toString}:0"
        val future = createSessionForItem(ObjectId.get.toString)(FakeRequest("", ""))
        val either = Await.result(future, Duration(1, TimeUnit.SECONDS))
        val (session, item) = either.right.get
        (session \ "id").asOpt[String] must beSome[String]
        (session \ "_id" \ "$oid").asOpt[String] must beNone
        (item \ "xhtml").asOpt[String] must_== Some("hi")
      }
    }
  }
}
