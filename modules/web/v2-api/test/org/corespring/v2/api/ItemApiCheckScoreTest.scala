package org.corespring.v2.api

import com.mongodb.casbah.Imports._
import org.bson.types.ObjectId
import org.corespring.platform.core.models.auth.{ApiClient, Permission}
import org.corespring.platform.core.models.item._
import org.corespring.platform.core.services.item.ItemService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.test.PlaySingleton
import org.corespring.v2.api.services.ScoreService
import org.corespring.v2.auth.ItemAuth
import org.corespring.v2.auth.models.{AuthMode, MockFactory, OrgAndOpts, PlayerAccessSettings}
import org.corespring.v2.errors.Errors._
import org.corespring.v2.errors.V2Error
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test.{FakeHeaders, FakeRequest}

import scala.concurrent.ExecutionContext
import scalaz.{Failure, Success, Validation}

class ItemApiCheckScoreTest extends Specification with Mockito with MockFactory {

  /**
   * We should not need to run the app for a unit test.
   * However the way the app is tied up (global Dao Objects) - we need to boot a play application.
   */
  PlaySingleton.start()

  def FakeJsonRequest(json: JsValue): FakeRequest[AnyContentAsJson] = FakeRequest("", "", FakeHeaders(Seq(CONTENT_TYPE -> Seq("application/json"))), AnyContentAsJson(json))

  def emptyPlayerDefinition = PlayerDefinition(Seq.empty, "", Json.obj(), "", None)

  case class checkScoreScope(
                              orgAndOpts: Validation[V2Error, OrgAndOpts] = Success(
                                OrgAndOpts(
                                  mockOrg,
                                  PlayerAccessSettings.ANYTHING,
                                  AuthMode.AccessToken)),
                              loadForReadResult: Validation[V2Error, Item] = Success(Item(playerDefinition = Some(emptyPlayerDefinition))),
                              scoreResult: Validation[V2Error, JsValue] = Success(Json.obj("score" -> 100)),
                              apiClient: Validation[V2Error, ApiClient] = Success(ApiClient(mockOrg.id, new ObjectId(), "secret"))) extends Scope {

    lazy val api = new ItemApi {

      override def defaultCollection(implicit identity: OrgAndOpts): Option[String] = ???

      override def itemService: ItemService = mock[ItemService]

      override def scoreService: ScoreService = {
        val m = mock[ScoreService]
        m.score(any[PlayerDefinition], any[JsValue]) returns scoreResult
        m
      }

      override def itemAuth: ItemAuth[OrgAndOpts] = {
        val m = mock[ItemAuth[OrgAndOpts]]
        m.loadForRead(anyString)(any[OrgAndOpts]) returns loadForReadResult
        m
      }

      override implicit def ec: ExecutionContext = ExecutionContext.Implicits.global

      override def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = orgAndOpts

      override def transform: (Item, Option[String]) => JsValue = (i, s) => Json.obj()

      override def getApiClient(request: RequestHeader): Validation[V2Error, ApiClient] = apiClient
    }
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
        loadForReadResult = Success(Item())
      ) {
        val result = api.checkScore("itemId")(FakeRequest("", "", FakeHeaders(), AnyContentAsJson(Json.obj())))
        val error = api.noPlayerDefinition(loadForReadResult.toEither.right.get.id)
        status(result) === error.statusCode
        contentAsJson(result) === error.json
      }

      "fail it the score service fails" in new checkScoreScope(
        scoreResult = Failure(generalError("couldn't get score"))
      ) {
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
