package org.corespring.v2.api

import global.Global
import org.bson.types.ObjectId
import org.corespring.it.IntegrationSpecification
import org.corespring.it.scopes._
import org.corespring.models.item.PlayerDefinition
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.auth.models.{ AuthMode, PlayerAccessSettings }
import org.corespring.v2.errors.Errors._
import org.specs2.specification.BeforeAfter
import play.api.libs.json.{ JsNumber, JsString, Json }
import play.api.mvc.{ AnyContentAsJson, AnyContent, Call, RequestHeader }

class ItemSessionApiIntegrationTest extends IntegrationSpecification with WithV2SessionHelper {
  val Routes = org.corespring.v2.api.routes.ItemSessionApi

  lazy val itemService = Global.main.itemService

  "ItemSessionApi" should {

    def mkCompoundError(rh: RequestHeader) = compoundError("Failed to identify an Organization from the request",
      Seq(
        noApiClientAndPlayerTokenInQueryString(rh),
        noToken(rh),
        noUserSession(rh)),
      UNAUTHORIZED)

    "when loading a session" should {

      s"return $UNAUTHORIZED for unknown user" in new unknownUser_getSession {
        val e = mkCompoundError(req)
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
        val e = mkCompoundError(req)
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

      s"adds identity data to the session" in new clientIdAndPlayerToken_createSession(
        Json.stringify(Json.toJson(PlayerAccessSettings.ANYTHING))) {
        val e = noOrgIdAndOptions(req)
        (contentAsJson(result) \ "id").asOpt[String].isDefined === true
        val sessionId = ((contentAsJson(result) \ "id")).as[String]
        val dbo = v2SessionHelper.findSession(sessionId).get
        val identity = (dbo \ "identity")
        (identity \ "orgId") === JsString(orgId.toString)
        (identity \ "authMode") === JsNumber(AuthMode.ClientIdAndPlayerToken.id)
        (identity \ "apiClient") === JsString(apiClient.clientId.toString)
      }
    }

    def playerDef(customScoring: Option[String] = None) = PlayerDefinition(
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
      customScoring)

    "when calling load score" should {

      s"1: return $OK and 100% - for multiple choice" in new token_loadScore(AnyContentAsJson(Json.obj())) {

        val item = itemService.findOneById(itemId).get
        //Note: We have to remove the qti or else the ItemTransformer will overrwrite the v2 data
        val update = item.copy(data = None, playerDefinition = Some(playerDef()))
        val resultString = s"""{"summary":{"maxPoints":1,"points":1.0,"percentage":100.0},"components":{"1":{"weight":1,"score":1.0,"weightedScore":1.0}}}"""
        val resultJson = Json.parse(resultString)
        itemService.save(update)
        println(s"playerDefinition: ${update.playerDefinition}")
        v2SessionHelper.update(sessionId, Json.obj("itemId" -> itemId.toString, "components" -> Json.obj(
          "1" -> Json.obj("answers" -> Json.arr("carrot")))))
        status(result) === OK
        contentAsJson(result) === resultJson
      }

      s"2: return $OK and 0% - for multiple choice" in new token_loadScore(AnyContentAsJson(Json.obj())) {
        val item = itemService.findOneById(itemId).get
        val update = item.copy(data = None, playerDefinition = Some(playerDef()))
        val resultString = s"""{"summary":{"maxPoints":1,"points":0.0,"percentage":0.0},"components":{"1":{"weight":1,"score":0.0,"weightedScore":0.0}}}"""
        val resultJson = Json.parse(resultString)
        itemService.save(update)
        v2SessionHelper.update(sessionId, Json.obj("itemId" -> itemId.toString, "components" -> Json.obj(
          "1" -> Json.obj("answers" -> Json.arr("banana")))))
        status(result) === OK
        contentAsJson(result) === resultJson
      }
    }

    "when calling load score with a custom scoring item" should {

      val customScoring = """
      exports.process = function(item, session, outcomes){
        if(session.components[1].answers.indexOf('carrot') !== -1){
          return { summary: { numcorrect: 1, score: 1.0}};
        } else {
          return { summary: { numcorrect: 0, score: 0}};
        }
      }
      """

      s"return $OK and 100% - for multiple choice" in new token_loadScore(AnyContentAsJson(Json.obj())) {

        val item = itemService.findOneById(itemId).get
        val update = item.copy(data = None, playerDefinition = Some(playerDef(Some(customScoring))))
        val resultString =
          s"""{ "components":{"1":{"weight":1,"score":1.0,"weightedScore":1.0}}, "summary":{"numcorrect" : 1, "score" : 1.0}}"""
        val resultJson = Json.parse(resultString)
        itemService.save(update)
        v2SessionHelper.update(sessionId, Json.obj("itemId" -> itemId.toString, "components" -> Json.obj(
          "1" -> Json.obj("answers" -> Json.arr("carrot")))))
        status(result) === OK
        contentAsJson(result) === resultJson
      }

    }

    "when calling load score with a custom scoring that uses outcomes" should {

      val customScoring = """
      exports.process = function(item, session, outcomes){
        if(outcomes["1"].correctness === "correct"){
          return { summary: { numcorrect: 1, score: 1.0}};
        } else {
          return { summary: { numcorrect: 0, score: 0}};
        }
      }
      """
      s"return $OK and 100% - for multiple choice" in new token_loadScore(AnyContentAsJson(Json.obj())) {

        val item = itemService.findOneById(itemId).get
        val update = item.copy(data = None, playerDefinition = Some(playerDef(Some(customScoring))))
        val resultString =
          s"""{ "components":{"1":{"weight":1,"score":1.0,"weightedScore":1.0}}, "summary":{"numcorrect" : 1, "score" : 1.0}}"""
        val resultJson = Json.parse(resultString)
        itemService.save(update)
        v2SessionHelper.update(sessionId, Json.obj("itemId" -> itemId.toString, "components" -> Json.obj(
          "1" -> Json.obj("answers" -> Json.arr("carrot")))))
        status(result) === OK
        contentAsJson(result) === resultJson
      }

      s"return $OK and 0% - for multiple choice" in new token_loadScore(AnyContentAsJson(Json.obj())) {

        val item = itemService.findOneById(itemId).get
        val update = item.copy(data = None, playerDefinition = Some(playerDef(Some(customScoring))))
        val resultString =
          s"""{ "components":{"1":{"weight":1,"score":0.0,"weightedScore":0.0}}, "summary":{"numcorrect" : 0, "score" : 0}}"""
        val resultJson = Json.parse(resultString)
        itemService.save(update)
        v2SessionHelper.update(sessionId, Json.obj("itemId" -> itemId.toString, "components" -> Json.obj(
          "1" -> Json.obj("answers" -> Json.arr("banana")))))
        status(result) === OK
        contentAsJson(result) === resultJson
      }

    }
  }

  "cloneSession" should {

    "return apiClient" in new cloneSession {
      (contentAsJson(result) \ "apiClient").as[String] must be equalTo (
        Global.main.apiClientService.findOneByOrgId(orgId).map(_.clientId).getOrElse(throw new Exception("Boop")).toString)
    }

    "return encrypted options, decryptable by provided apiClient" in new cloneSession {
      val apiClientId = (contentAsJson(result) \ "apiClient").as[String]
      val encrypter = Global.main.apiClientEncryptionService
      encrypter.decrypt(apiClientId, (contentAsJson(result) \ "options").as[String]) must be equalTo (
        Some(ItemSessionApi.clonedSessionOptions.toString))
    }

    "return organization name" in new cloneSession {
      (contentAsJson(result) \ "organization").as[String] must be equalTo (organization.name)
    }

  }

  class cloneSession extends BeforeAfter with sessionLoader with TokenRequestBuilder with orgWithAccessTokenItemAndSession {
    override def getCall(sessionId: ObjectId): Call = Routes.cloneSession(sessionId.toString)
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

  class clientIdAndPlayerToken_createSession(val playerToken: String, val skipDecryption: Boolean = true)
    extends createSession
    with clientIdAndPlayerToken
    with IdAndPlayerTokenRequestBuilder {
    override def getCall(itemId: VersionedId[ObjectId]): Call = Routes.create(itemId)
  }

  class token_getSession extends BeforeAfter with sessionLoader with TokenRequestBuilder with orgWithAccessTokenItemAndSession {
    override def getCall(sessionId: ObjectId): Call = Routes.get(sessionId.toString)
  }

  class token_loadScore(json: AnyContent) extends BeforeAfter with sessionLoader with TokenRequestBuilder with orgWithAccessTokenItemAndSession {
    override def getCall(sessionId: ObjectId): Call = Routes.loadScore(sessionId.toString)
    override def requestBody = json
  }

  class clientIdAndPlayerToken_getSession(val playerToken: String, val skipDecryption: Boolean = true)
    extends clientIdAndPlayerToken
    with IdAndPlayerTokenRequestBuilder
    with sessionLoader
    with HasSessionId
    with WithV2SessionHelper {
    override def getCall(sessionId: ObjectId): Call = Routes.get(sessionId.toString)

    lazy override val sessionId: ObjectId = v2SessionHelper.create(itemId)

    override def before = {
      super.before
    }

    override def after = {
      super.after
      v2SessionHelper.delete(sessionId)
    }
  }
}
