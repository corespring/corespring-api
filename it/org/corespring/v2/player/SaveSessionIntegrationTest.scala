package org.corespring.v2.player

import com.mongodb.DBObject
import org.bson.types.ObjectId
import org.corespring.it.IntegrationSpecification
import org.corespring.test.helpers.models.V2SessionHelper
import org.corespring.v2.auth.models.{AuthMode, PlayerAccessSettings}
import org.corespring.v2.player.scopes._
import play.api.libs.json.{JsNumber, JsString, JsObject, Json}
import play.api.mvc.{AnyContentAsJson, Request}

class SaveSessionIntegrationTest extends IntegrationSpecification {

  "saving a session" should {

    "work for token" in new token_saveSession() {
      status(result) ==== OK
    }

    "fail for client id and bad options" in new clientId_saveSession("Let me in") {
      status(result) ==== UNAUTHORIZED
    }

    "work for client id and options" in new clientId_saveSession(Json.stringify(Json.toJson(PlayerAccessSettings.ANYTHING))) {
      status(result) ==== OK
    }

    "save identity data to the session" in new clientId_saveSession(Json.stringify(Json.toJson(PlayerAccessSettings.ANYTHING))) {
      val resultJson = contentAsJson(result)
      (resultJson \ "identity").asOpt[JsObject] === None
      val sessionDbo = V2SessionHelper.findSession(sessionId.toString).get
      val identity = (sessionDbo \"identity")
      (identity \ "orgId") === JsString(orgId.toString)
      (identity \ "authMode") === JsNumber(AuthMode.ClientIdAndPlayerToken.id)
      (identity \ "apiClient") === JsString(apiClient.clientId.toString)
    }

  }

  trait saveSession extends { self: RequestBuilder with HasSessionId =>
    import org.corespring.container.client.controllers.resources.routes.Session

    lazy val result = {
      val call = Session.saveSession(sessionId.toString)
      val request = makeRequest(call).asInstanceOf[Request[AnyContentAsJson]]
      logger.trace(s"load session make request: ${request.uri}")
      route(request).getOrElse(throw new RuntimeException("Error routing Session.loadEverything"))
    }
  }

  class token_saveSession extends saveSession with orgWithAccessTokenItemAndSession with TokenRequestBuilder {
    override def requestBody = AnyContentAsJson(Json.obj())

    override def after : Any = {
      super.after
      println("[token_saveSession] - after")
      V2SessionHelper.delete(sessionId)
    }
  }

  class clientId_saveSession(val playerToken: String, val skipDecryption: Boolean = true) extends saveSession with clientIdAndPlayerToken with IdAndPlayerTokenRequestBuilder with HasSessionId {
    override def requestBody = AnyContentAsJson(Json.obj())
    override lazy val sessionId: ObjectId = V2SessionHelper.create(itemId)

    override def after : Any = {
      super.after
      println("[clientId_saveSession] - after")
      V2SessionHelper.delete(sessionId)
    }
  }

}
