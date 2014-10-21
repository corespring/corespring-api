package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.it.IntegrationSpecification
import org.corespring.platform.core.models.item.PlayerDefinition
import org.corespring.platform.core.services.item.ItemServiceWired
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.test.helpers.models.{ ItemHelper, V2SessionHelper }
import org.corespring.v2.auth.models.PlayerAccessSettings
import org.corespring.v2.errors.Errors._
import org.corespring.v2.player.scopes._
import org.specs2.specification.BeforeAfter
import play.api.libs.json.Json
import play.api.mvc.{ AnyContentAsJson, AnyContent, Call }

class ItemSessionApiTest extends IntegrationSpecification {
  val Routes = org.corespring.v2.api.routes.ItemSessionApi

  "ItemSessionApi" should {

    "when loading a session" should {

      s"return $UNAUTHORIZED for unknown user" in new unknownUser_getSession {

        val e = compoundError("Failed to identify an Organization from the request",
          Seq(noToken(req), noApiClientAndPlayerTokenInQueryString(req)),
          UNAUTHORIZED)

        contentAsJson(result) === e.json
        status(result) === e.statusCode
      }

      s"return $OK for token based request" in new token_getSession {
        status(result) === OK
        contentAsJson(result) === Json.obj("id" -> sessionId.toString(), "itemId" -> itemId.toString())
      }

      s"return $OK for client id and player token" in new clientIdAndPlayerToken_getSession(Json.stringify(Json.toJson(PlayerAccessSettings.ANYTHING)), true) {
        status(result) === OK
        contentAsJson(result) === Json.obj("id" -> sessionId.toString(), "itemId" -> itemId.toString())
      }

    }

    "when creating a session" should {

      s"return $BAD_REQUEST for unknown user" in new unknownUser_createSession {
        val e = compoundError("Failed to identify an Organization from the request",
          Seq(noToken(req), noApiClientAndPlayerTokenInQueryString(req)),
          UNAUTHORIZED)
        status(result) === e.statusCode
        contentAsJson(result) === e.json
      }

      s"return $OK for token" in new token_createSession {
        val e = noOrgIdAndOptions(req)
        (contentAsJson(result) \ "id").asOpt[String].isDefined === true
        status(result) === OK
      }

      s"return $OK for client id and player token" in new clientIdAndPlayerToken_createSession(
        Json.stringify(Json.toJson(PlayerAccessSettings.ANYTHING))) {
        val e = noOrgIdAndOptions(req)
        (contentAsJson(result) \ "id").asOpt[String].isDefined === true
        status(result) === OK
      }

    }

    "when calling load score" should {

      def playerDef = PlayerDefinition(
        Seq.empty,
        "html",
        Json.obj(
          "1" -> Json.obj(
            "componentType" -> "corespring-multiple-choice",
            "correctResponse" -> Json.obj("value" -> Json.arr("carrot")),
            "model" -> Json.obj(
              "config" -> Json.obj(
                "singleChoice" -> true),
              "prompt" -> "Carrot?",
              "choices" -> Json.arr(
                Json.obj("label" -> "carrot", "value" -> "carrot"),
                Json.obj("label" -> "banana", "value" -> "banana"))))),
        "",
        None)

      s"return $OK and 100% - for multiple choice" in new token_loadScore(AnyContentAsJson(Json.obj())) {

        val item = ItemServiceWired.findOneById(itemId).get
        val update = item.copy(playerDefinition = Some(playerDef))
        val resultString = s"""{"summary":{"maxPoints":1,"points":1.0,"percentage":100.0},"components":{"1":{"weight":1,"score":1.0,"weightedScore":1.0}}}"""
        val resultJson = Json.parse(resultString)
        ItemServiceWired.save(update)
        V2SessionHelper.update(sessionId, Json.obj("itemId" -> itemId.toString, "components" -> Json.obj(
          "1" -> Json.obj("answers" -> Json.arr("carrot")))))
        status(result) === OK
        contentAsJson(result) === resultJson
      }

      s"return $OK and 0% - for multiple choice" in new token_loadScore(AnyContentAsJson(Json.obj())) {
        val item = ItemServiceWired.findOneById(itemId).get
        val update = item.copy(playerDefinition = Some(playerDef))
        val resultString = s"""{"summary":{"maxPoints":1,"points":0.0,"percentage":0.0},"components":{"1":{"weight":1,"score":0.0,"weightedScore":0.0}}}"""
        val resultJson = Json.parse(resultString)
        ItemServiceWired.save(update)
        V2SessionHelper.update(sessionId, Json.obj("itemId" -> itemId.toString, "components" -> Json.obj(
          "1" -> Json.obj("answers" -> Json.arr("banana")))))
        status(result) === OK
        contentAsJson(result) === resultJson
      }
    }
  }

  class unknownUser_getSession extends sessionLoader with orgWithAccessTokenItemAndSession with PlainRequestBuilder {
    override def getCall(sessionId: ObjectId): Call = Routes.get(sessionId.toString)
  }

  class unknownUser_createSession extends createSession with orgWithAccessTokenItemAndSession with PlainRequestBuilder {
    override def getCall(itemId: VersionedId[ObjectId]): Call = Routes.create(itemId)
  }

  class token_createSession extends createSession with orgWithAccessTokenItemAndSession with TokenRequestBuilder {
    override def getCall(itemId: VersionedId[ObjectId]): Call = Routes.create(itemId)
  }

  class clientIdAndPlayerToken_createSession(val playerToken: String, val skipDecryption: Boolean = true) extends createSession with clientIdAndPlayerToken with IdAndPlayerTokenRequestBuilder {
    override def getCall(itemId: VersionedId[ObjectId]): Call = Routes.create(itemId)
  }

  class token_getSession extends BeforeAfter with sessionLoader with TokenRequestBuilder with orgWithAccessTokenItemAndSession {
    override def getCall(sessionId: ObjectId): Call = Routes.get(sessionId.toString)
  }

  class token_loadScore(json: AnyContent) extends BeforeAfter with sessionLoader with TokenRequestBuilder with orgWithAccessTokenItemAndSession {
    override def getCall(sessionId: ObjectId): Call = Routes.loadScore(sessionId.toString)
    override def requestBody = json
  }

  class clientIdAndPlayerToken_getSession(val playerToken: String, val skipDecryption: Boolean = true) extends clientIdAndPlayerToken with IdAndPlayerTokenRequestBuilder with sessionLoader with HasSessionId {
    override def getCall(sessionId: ObjectId): Call = Routes.get(sessionId.toString)

    lazy override val sessionId: ObjectId = V2SessionHelper.create(itemId)

    override def before = {
      super.before
    }

    override def after = {
      super.after
      V2SessionHelper.delete(sessionId)
    }
  }
}
