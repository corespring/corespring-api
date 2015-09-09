package org.corespring.v2.player.hooks

import java.util.concurrent.TimeUnit

import org.bson.types.ObjectId
import org.corespring.models.item.{ Item, PlayerDefinition }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.qtiToV2.transformers.ItemTransformer
import org.corespring.services.item.ItemService
import org.corespring.v2.auth.SessionAuth
import org.corespring.v2.auth.models.{ AuthMode, OrgAndOpts, PlayerAccessSettings }
import org.corespring.v2.errors.Errors.cantLoadSession
import org.corespring.v2.errors.V2Error
import org.corespring.v2.player.V2PlayerIntegrationSpec
import org.specs2.specification.Scope
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc._
import play.api.test.FakeRequest

import scalaz.{ Failure, Success, Validation }

class PlayerHooksTest extends V2PlayerIntegrationSpec {

  lazy val orgAndOpts = OrgAndOpts(mockOrg(), PlayerAccessSettings.ANYTHING, AuthMode.AccessToken, None)

  class defaultScope(orgAndOptsResult: Validation[V2Error, OrgAndOpts] = Success(orgAndOpts))
    extends Scope with StubJsonFormatting {

    val itemService = mock[ItemService]
    val itemTransformer = {
      val m = mock[ItemTransformer]
      m.loadItemAndUpdateV2(any[VersionedId[ObjectId]]) answers { (vid) =>
        Some(Item(collectionId = mockCollectionId.toString, id = vid.asInstanceOf[VersionedId[ObjectId]], playerDefinition = Some(PlayerDefinition("hi"))))
      }
      m
    }
    val sessionAuth = {
      val m = mock[SessionAuth[OrgAndOpts, PlayerDefinition]]
      m.canCreate(any[String])(any[OrgAndOpts]) returns Success(true)
      m.create(any[JsValue])(any[OrgAndOpts]) answers { (obj, m) =>
        val sessionJson = obj.asInstanceOf[Array[Any]](0).asInstanceOf[JsValue]
        val idString = (sessionJson \ "_id" \ "$oid").as[String]
        println(s"idString: $idString")
        Success(new ObjectId(idString))
      }
      m
    }

    val playerAssets = mock[PlayerAssets]

    def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = orgAndOptsResult

    val hooks = new PlayerHooks(itemService, itemTransformer, sessionAuth, jsonFormatting, playerAssets, getOrgAndOptions)

  }

  class hooksScope(orgAndOptsResult: Validation[V2Error, OrgAndOpts] = Success(orgAndOpts),
    loadForReadResult: Validation[V2Error, (JsValue, PlayerDefinition)] = Success(Json.obj() -> PlayerDefinition(Seq.empty, "", Json.obj(), "", None)))
    extends defaultScope(orgAndOptsResult) {

    sessionAuth.loadForRead(any[String])(any[OrgAndOpts]) returns loadForReadResult
  }

  "PlayerHooks" should {

    import scala.concurrent._
    import scala.concurrent.duration._

    val cantLoadSessionError = cantLoadSession("bad session")

    "loadSessionAndItem" should {
      "pass back the status code" in new hooksScope(loadForReadResult = Failure(cantLoadSessionError)) {
        hooks.loadSessionAndItem("sessionId")(FakeRequest("", "")) must equalTo(Left(cantLoadSessionError.statusCode -> cantLoadSessionError.message)).await
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
        val future = hooks.createSessionForItem(ObjectId.get.toString)(FakeRequest("", ""))
        val either = Await.result(future, Duration(1, TimeUnit.SECONDS))
        val (session, item) = either.right.get
        (session \ "id").asOpt[String] must beSome[String]
        (session \ "_id" \ "$oid").asOpt[String] must beSome[String]
        (item \ "xhtml").asOpt[String] must_== Some("hi")
      }
    }
  }
}
