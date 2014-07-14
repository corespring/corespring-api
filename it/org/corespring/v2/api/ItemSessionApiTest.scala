package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.it.IntegrationSpecification
import org.corespring.test.SecureSocialHelpers
import org.corespring.test.helpers.models.V2SessionHelper
import org.corespring.v2.auth.models.PlayerOptions
import org.corespring.v2.errors.Errors.noOrgIdAndOptions
import org.corespring.v2.player.scopes._
import org.specs2.specification.BeforeAfter
import play.api.libs.json.Json
import play.api.mvc.Call

class ItemSessionApiTest extends IntegrationSpecification {
  val Routes = org.corespring.v2.api.routes.ItemSessionApi

  "ItemSessionApi" should {

    "when loading a session" should {

      s"return $BAD_REQUEST for unknown user" in new unknownUser_getSession {
        val e = noOrgIdAndOptions(req)
        status(result) === e.statusCode
        contentAsJson(result) === e.json
      }

      s"return $OK for token based request" in new token_getSession {
        status(result) === OK
        contentAsJson(result) === Json.obj("id" -> sessionId.toString(), "itemId" -> itemId.toString())
      }

      s"return $OK for user based request" in new user_getSession {
        status(result) === OK
        contentAsJson(result) === Json.obj("id" -> sessionId.toString(), "itemId" -> itemId.toString())
      }

      s"return $OK for client id and options" in new clientIdAndOptions_getSession(Json.stringify(Json.toJson(PlayerOptions.ANYTHING)), true) {
        status(result) === OK
        contentAsJson(result) === Json.obj("id" -> sessionId.toString(), "itemId" -> itemId.toString())
      }

    }
  }

  class unknownUser_getSession extends sessionLoader with orgWithAccessTokenItemAndSession with PlainRequestBuilder {
    override def getCall(sessionId: ObjectId): Call = Routes.get(sessionId.toString)
  }

  class user_getSession extends sessionLoader with SessionRequestBuilder with userWithItemAndSession with SecureSocialHelpers {
    override def getCall(sessionId: ObjectId): Call = Routes.get(sessionId.toString)
  }

  class token_getSession extends BeforeAfter with sessionLoader with TokenRequestBuilder with orgWithAccessTokenItemAndSession {
    override def getCall(sessionId: ObjectId): Call = Routes.get(sessionId.toString)
  }

  class clientIdAndOptions_getSession(val options: String, val skipDecryption: Boolean = true) extends clientIdAndOptions with IdAndOptionsRequestBuilder with sessionLoader with HasSessionId {
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
