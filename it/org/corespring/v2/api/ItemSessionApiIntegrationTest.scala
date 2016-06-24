package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.it.IntegrationSpecification
import org.corespring.it.scopes._
import org.corespring.models.item.PlayerDefinition
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.auth.models.{ AuthMode, PlayerAccessSettings }
import org.corespring.v2.errors.Errors._
import org.specs2.specification.BeforeAfter
import play.api.libs.json.{ JsNumber, JsString, Json }
import play.api.mvc.{ AnyContent, AnyContentAsJson, Call }

class ItemSessionApiIntegrationTest extends IntegrationSpecification with WithV2SessionHelper {
  val Routes = org.corespring.v2.api.routes.ItemSessionApi

  lazy val itemService = main.itemService

  "ItemSessionApi" should {

    "when loading a session" should {

      s"return $UNAUTHORIZED for unknown user" in new unknownUser_getSession {
        val e = noToken(req)
        println(contentAsString(result))
        contentAsJson(result) === e.json
        status(result) === e.statusCode
      }

      s"return $OK for token based request" in new token_getSession {
        status(result) === OK
        contentAsJson(result) === Json.obj("id" -> sessionId.toString(), "itemId" -> itemId.toString())
      }

      s"return $UNAUTHORIZED for client id and player token" in new clientIdAndPlayerToken_getSession(Json.stringify(Json.toJson(PlayerAccessSettings.ANYTHING)), true) {
        status(result) === UNAUTHORIZED
      }

    }

    "when creating a session" should {

      s"return $BAD_REQUEST for unknown user" in new unknownUser_createSession {
        val e = noToken(req)
        status(result) === e.statusCode
        contentAsJson(result) === e.json
      }

      s"only creates 1 session" in new orgWithAccessTokenAndItem with TokenRequestBuilder {
        val count = v2SessionHelper.count(itemId)
        count must_== 0
        val Routes = org.corespring.v2.api.routes.ItemSessionApi
        val req = makeRequest(Routes.create(itemId))
        val result = route(req).get
        status(result) must_== OK
        val newCount = v2SessionHelper.count(itemId)
        newCount must_== 1
      }

      s"return $OK for token" in new token_createSession {
        (contentAsJson(result) \ "id").asOpt[String].isDefined === true
        status(result) === OK
      }

      s"return $UNAUTHORIZED for client id and player token" in new clientIdAndPlayerToken_createSession(
        Json.stringify(Json.toJson(PlayerAccessSettings.ANYTHING))) {
        status(result) === UNAUTHORIZED
      }

      s"adds identity data to the session" in new token_createSession {
        val e = noOrgIdAndOptions(req)
        (contentAsJson(result) \ "id").asOpt[String].isDefined === true
        val newSessionId = ((contentAsJson(result) \ "id")).as[String]
        val dbo = v2SessionHelper.findSession(newSessionId).get
        val identity = (dbo \ "identity")
        (identity \ "orgId") === JsString(orgId.toString)
        (identity \ "authMode") === JsNumber(AuthMode.AccessToken.id)
        (identity \ "apiClient") === JsString(apiClient.clientId.toString)
      }
    }
  }

  "cloneSession" should {

    "return apiClient" in new cloneSession {
      (contentAsJson(result) \ "apiClient").as[String] must be equalTo (
        main.apiClientService.findByOrgId(orgId).headOption.map(_.clientId).getOrElse(throw new Exception("Boop")).toString)
    }

    "return encrypted options, decryptable by provided apiClient" in new cloneSession {
      val apiClientId = (contentAsJson(result) \ "apiClient").as[String]
      val encrypter = main.apiClientEncryptionService
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
