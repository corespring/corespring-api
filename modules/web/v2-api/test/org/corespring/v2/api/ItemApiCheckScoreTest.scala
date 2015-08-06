package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.itemSearch.ItemIndexService
import org.corespring.models.item._
import org.corespring.models.json.JsonFormatting
import org.corespring.services.OrganizationService
import org.corespring.services.item.ItemService
import org.corespring.test.PlaySingleton
import org.corespring.v2.api.services.ScoreService
import org.corespring.v2.auth.ItemAuth
import org.corespring.v2.auth.models.{ AuthMode, MockFactory, OrgAndOpts, PlayerAccessSettings }
import org.corespring.v2.errors.Errors._
import org.corespring.v2.errors.V2Error
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.{ JsObject, JsValue, Json }
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test.{ FakeHeaders, FakeRequest }

import scala.concurrent.ExecutionContext
import scalaz.{ Failure, Success, Validation }

class ItemApiCheckScoreTest extends Specification with Mockito with MockFactory {

  def FakeJsonRequest(json: JsValue): FakeRequest[AnyContentAsJson] = FakeRequest("", "", FakeHeaders(Seq(CONTENT_TYPE -> Seq("application/json"))), AnyContentAsJson(json))

  def emptyPlayerDefinition = PlayerDefinition.empty

  lazy val collectionId = ObjectId.get

  case class checkScoreScope(
    override val orgAndOpts: Validation[V2Error, OrgAndOpts] = Success(mockOrgAndOpts()),
    loadForReadResult: Validation[V2Error, Item] = Success(Item(collectionId = collectionId.toString, playerDefinition = Some(emptyPlayerDefinition))),
    scoreResult: Validation[V2Error, JsValue] = Success(Json.obj("score" -> 100))) extends ItemApiScope {

    mockItemAuth.loadForRead(anyString)(any[OrgAndOpts]) returns loadForReadResult
    mockScoreService.score(any[PlayerDefinition], any[JsValue]) returns scoreResult
  }

  "V2 - ItemApi" should {

    "when calling check-score.json" should {

      "fail it the org and opts aren't found" in new checkScoreScope(
        orgAndOpts = Failure(generalError("no org and opts"))) {
        val result = api.checkScore("itemId")(FakeRequest("", ""))
        val error = orgAndOpts.toEither.left.get
        status(result) === error.statusCode
        contentAsJson(result) === error.json
      }

      "fail it the json body is empty" in new checkScoreScope() {
        val result = api.checkScore("itemId")(FakeRequest("", ""))
        val error = noJson
        status(result) === error.statusCode
        contentAsJson(result) === error.json
      }

      "fail it the item isn't loaded" in new checkScoreScope(
        loadForReadResult = Failure(generalError("No item"))) {
        val result = api.checkScore("itemId")(FakeRequest("", "", FakeHeaders(), AnyContentAsJson(Json.obj())))
        val error = loadForReadResult.toEither.left.get
        status(result) === error.statusCode
        contentAsJson(result) === error.json
      }

      "fail if there is no player definition" in new checkScoreScope(
        loadForReadResult = Success(Item(collectionId.toString))) {
        val result = api.checkScore("itemId")(FakeRequest("", "", FakeHeaders(), AnyContentAsJson(Json.obj())))
        val error = api.noPlayerDefinition(loadForReadResult.toEither.right.get.id)
        status(result) === error.statusCode
        contentAsJson(result) === error.json
      }

      "fail it the score service fails" in new checkScoreScope(
        scoreResult = Failure(generalError("couldn't get score"))) {
        val result = api.checkScore("itemId")(FakeRequest("", "", FakeHeaders(), AnyContentAsJson(Json.obj())))
        val error = scoreResult.toEither.left.get //api.noPlayerDefinition(loadForReadResult.toEither.right.get.id)
        status(result) === error.statusCode
        contentAsJson(result) === error.json
      }

      "work" in new checkScoreScope() {
        val result = api.checkScore("itemId")(
          FakeRequest("", "",
            FakeHeaders(),
            AnyContentAsJson(Json.obj())))
        println(contentAsString(result))
        status(result) === OK
        contentAsJson(result) === scoreResult.toEither.right.get
      }
    }
  }
}
