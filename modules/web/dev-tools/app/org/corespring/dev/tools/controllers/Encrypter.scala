package org.corespring.dev.tools.controllers

import org.bson.types.ObjectId
import org.corespring.common.encryption.AESCrypto
import org.corespring.platform.core.controllers.auth.OAuthProvider
import org.corespring.platform.core.encryption.{EncryptionFailure, EncryptionSuccess, OrgEncrypter}
import org.corespring.platform.core.models.auth.ApiClient
import play.api.libs.json.Json
import play.api.mvc.Controller

object Encrypter extends Controller {

  def encryptPage = DevToolsAction{ request =>

    val call = org.corespring.dev.tools.controllers.routes.Encrypter.encrypt(":orgId")
    Ok(org.corespring.dev.tools.views.html.Encrypt(call.url))
  }

  def encrypt(orgId: String) = DevToolsAction { request =>

    val encrypter = new OrgEncrypter(new ObjectId(orgId), AESCrypto)

    if(ApiClient.findOneByOrgId(new ObjectId(orgId)).isEmpty){
      println("need to create an api client for org")
      OAuthProvider.createApiClient(new ObjectId(orgId))
    }

    println(request.body.asJson)

    val result = for {
      json <- request.body.asJson
      result <- encrypter.encrypt(Json.stringify(json))
    } yield result

    result.map{
      case EncryptionSuccess(id, data, _) => Ok(Json.obj("apiClient" -> id, "options" -> data))
      case EncryptionFailure(msg, e) => Ok(Json.obj("error" -> msg))
    }.getOrElse(BadRequest(""))
  }
}
