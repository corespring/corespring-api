package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.models.item._
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.Errors._
import org.corespring.v2.errors.V2Error
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test.{ FakeHeaders, FakeRequest }

import scalaz.{ Failure, Success, Validation }

class ItemApiCheckScoreTest extends ItemApiSpec {

  def emptyPlayerDefinition = PlayerDefinition.empty

  lazy val collectionId = ObjectId.get

  case class checkScoreScope(
    override val orgAndOpts: Validation[V2Error, OrgAndOpts] = Success(mockOrgAndOpts()),
    val loadForReadResult: Validation[V2Error, Item] = Success(Item(collectionId = collectionId.toString, playerDefinition = Some(emptyPlayerDefinition))),
    val scoreResult: Validation[V2Error, JsValue] = Success(Json.obj("score" -> 100))) extends ItemApiScope {

    mockItemAuth.loadForRead(anyString)(any[OrgAndOpts]) returns loadForReadResult
    mockScoreService.score(any[PlayerDefinition], any[JsValue]) returns scoreResult

    def error[A](v: Validation[V2Error, A]) = v.swap.toOption.get

    def jsonReq = FakeJsonRequest()
  }

  "V2 - ItemApi" should {

    "when calling check-score.json" should {

      "fail it the org and opts aren't found" in new checkScoreScope(
        orgAndOpts = Failure(generalError("no org and opts"))) {
        val e = error(orgAndOpts)
        api.checkScore("itemId")(FakeRequest("", "")) must beCodeAndJson(e.statusCode, e.json)
      }

      "fail it the json body is empty" in new checkScoreScope() {
        api.checkScore("itemId")(FakeRequest("", "")) must beCodeAndJson(noJson.statusCode, noJson.json)
      }

      "fail it the item isn't loaded" in new checkScoreScope(
        loadForReadResult = Failure(generalError("No item"))) {
        val e = error(loadForReadResult)
        api.checkScore("itemId")(jsonReq) must beCodeAndJson(e.statusCode, e.json)
      }

      "fail if there is no player definition" in new checkScoreScope(
        loadForReadResult = Success(Item(collectionId.toString, id = itemId))) {
        val e = api.noPlayerDefinition(itemId)
        api.checkScore("itemId")(jsonReq) must beCodeAndJson(e.statusCode, e.json)
      }

      "fail it the score service fails" in new checkScoreScope(
        scoreResult = Failure(generalError("couldn't get score"))) {
        val e = error(scoreResult)
        api.checkScore("itemId")(jsonReq) must beCodeAndJson(e.statusCode, e.json)
      }

      "return the score" in new checkScoreScope() {
        val result = api.checkScore("itemId")(
          FakeRequest("", "",
            FakeHeaders(),
            AnyContentAsJson(Json.obj())))
        println(contentAsString(result))
        result must beCodeAndJson(OK, scoreResult.toOption.get)
      }
    }
  }
}
