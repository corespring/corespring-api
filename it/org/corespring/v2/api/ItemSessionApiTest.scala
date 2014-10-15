package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.it.IntegrationSpecification
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.test.helpers.models.V2SessionHelper
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

    "when calling check score" should {
      s"return $OK" in new token_checkScore(AnyContentAsJson(Json.obj())) {
        println(contentAsString(result))
        status(result) === OK
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

  class token_checkScore(json: AnyContent) extends BeforeAfter with sessionLoader with TokenRequestBuilder with orgWithAccessTokenItemAndSession {
    override def getCall(sessionId: ObjectId): Call = Routes.checkScore(sessionId.toString)
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
