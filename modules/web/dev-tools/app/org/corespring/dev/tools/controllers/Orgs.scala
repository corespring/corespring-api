package org.corespring.dev.tools.controllers

import com.mongodb.casbah.commons.MongoDBObject
import org.corespring.dev.tools.DevTools
import org.corespring.platform.core.models.auth.{ AccessToken, ApiClient }
import org.corespring.platform.core.models.{ Organization => ModelOrg }
import play.api.libs.json.{ JsArray, JsObject, Json }
import play.api.mvc.{ Action, Controller }

object Organization extends Controller {

  def list = Action { request =>

    if (DevTools.enabled) {

      val orgs = ModelOrg.findAll.toSeq

      val out = orgs.map { o =>

        val clients = ApiClient.find(MongoDBObject("orgId" -> o.id)).toSeq

        val tokens = AccessToken.find(MongoDBObject("organization" -> o.id)).toSeq
        val json = Json.toJson(o).as[JsObject]

        json ++ Json.obj("apiClients" -> Json.toJson(JsArray(clients.map { c =>
          Json.obj(
            "clientId" -> c.clientId.toString,
            "secret" -> c.clientSecret)
        }))) ++ Json.obj("accessTokens" -> Json.toJson(JsArray(tokens.map { t =>
          Json.obj("token" -> t.tokenId)
        })))

      }
      Ok(JsArray(out))
    } else {
      BadRequest("")
    }

  }
}
