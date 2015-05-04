package org.corespring.v2.player.hooks

import java.util.concurrent.TimeUnit

import org.bson.types.ObjectId
import org.corespring.platform.core.models.item.PlayerDefinition
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
import play.api.mvc.{ SimpleResult, AnyContent, Request, RequestHeader }
import play.api.test.{ WithApplication, FakeRequest }

import scalaz.{ Failure, Success, Validation }

class PlayerHooksTest extends Specification with Mockito with MockFactory {

  lazy val orgAndOpts = OrgAndOpts(mockOrg(), PlayerAccessSettings.ANYTHING, AuthMode.AccessToken, None)

  case class hooksScope(orgAndOptsResult: Validation[V2Error, OrgAndOpts] = Success(orgAndOpts),
    loadForReadResult: Validation[V2Error, (JsValue, PlayerDefinition)] = Success(Json.obj() -> PlayerDefinition(Seq.empty, "", Json.obj(), "", None)))
    extends WithApplication with Scope {

    val hooks = new PlayerHooks {

      override def itemService: ItemService = {
        val m = mock[ItemService]
        m
      }

      override def itemTransformer: ItemTransformer = {
        val m = mock[ItemTransformer]
        m
      }

      override def auth: SessionAuth[OrgAndOpts, PlayerDefinition] = {
        val m = mock[SessionAuth[OrgAndOpts, PlayerDefinition]]
        m.loadForRead(any[String])(any[OrgAndOpts]) returns loadForReadResult
        m
      }

      override def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = orgAndOptsResult

      override def loadFile(id: String, path: String)(request: Request[AnyContent]): SimpleResult = ???
    }
  }

  "PlayerHooks" should {

    import scala.concurrent._
    import scala.concurrent.duration._

    val cantLoadSessionError = cantLoadSession("bad session")
    "loadItem" should {
      "pass back the status code" in new hooksScope(loadForReadResult = Failure(cantLoadSessionError)) {
        val future = hooks.load("sessionId")(FakeRequest("", ""))
        val either = Await.result(future, Duration(1, TimeUnit.SECONDS))
        either === Left(cantLoadSessionError.statusCode -> cantLoadSessionError.message)
      }
    }

    "loadSessionAndItem" should {
      "pass back the status code" in new hooksScope(loadForReadResult = Failure(cantLoadSessionError)) {
        val future = hooks.loadSessionAndItem("sessionId")(FakeRequest("", ""))
        val either = Await.result(future, Duration(1, TimeUnit.SECONDS))
        either === Left(cantLoadSessionError.statusCode -> cantLoadSessionError.message)
      }
    }
  }
}
