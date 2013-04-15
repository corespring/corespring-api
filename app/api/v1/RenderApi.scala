package api.v1

import controllers.auth.{AESCrypto, BaseApi, RendererContext}
import models.auth.{RenderConstraints, ApiClient}
import play.api.libs.json.{Json, JsString, JsObject}

object RenderApi extends BaseApi{

  def getRenderKey = ApiAction {request =>
    request.body.asJson match {
      case Some(jsrc) => {
        val renderConstraints = Json.fromJson[RenderConstraints](jsrc)
        ApiClient.findOneByOrgId(request.ctx.organization) match {
          case Some(apiClient) => {
            val encryptedConstraints = AESCrypto.encryptAES(Json.toJson(renderConstraints).toString(),apiClient.clientSecret)
            val key = apiClient.clientId.toString+RendererContext.keyDelimeter+encryptedConstraints
            Ok(JsObject(Seq("key" -> JsString(key))))
          }
          case None => BadRequest(JsObject(Seq("message" -> JsString("no api client found! this should never occur"))))
        }
      }
      case None => BadRequest(JsObject(Seq("message" -> JsString("your request must contain json properties containing the constraints of the key"))))
    }
  }
}
