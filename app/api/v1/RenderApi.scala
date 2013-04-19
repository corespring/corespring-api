package api.v1

import controllers.auth._
import models.auth.{ApiClient}
import play.api.libs.json._
import org.bson.types.ObjectId
import scala.Some
import play.api.mvc.{Action, Result, AnyContent}
import testplayer.controllers.ItemPlayer
import models.itemSession.ItemSessionSettings
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import scala.Some

object RenderApi extends BaseApi{
  import BaseRender._
  def renderOptions = ApiAction {request =>
    request.body.asJson match {
      case Some(jsoptions) => {
        val options = Json.fromJson[RenderOptions](jsoptions)
        ApiClient.findOneByOrgId(request.ctx.organization) match {
          case Some(apiClient) => {
            val encryptedOptions = AESCrypto.encryptAES(Json.toJson(options).toString(),apiClient.clientSecret)
            Ok(JsObject(Seq(
              "clientId" -> JsString(apiClient.clientId.toString),
              "options" -> JsString(encryptedOptions)
            ))).withSession(RendererHeader -> (apiClient.clientId.toString+Delimeter+encryptedOptions))
          }
          case None => BadRequest(JsObject(Seq("message" -> JsString("no api client found! this should never occur"))))
        }
      }
      case None => BadRequest(JsObject(Seq("message" -> JsString("your request must contain json properties containing the constraints of the key"))))
    }
  }
}
