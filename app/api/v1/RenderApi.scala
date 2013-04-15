package api.v1

import controllers.auth._
import models.auth.{ApiClient}
import play.api.libs.json.{Json, JsString, JsObject}
import org.bson.types.ObjectId
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import scala.Some
import play.api.mvc.{Action, Result, AnyContent}
import testplayer.controllers.ItemPlayer

object RenderApi extends BaseApi with BaseRender{

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
  /*
 POST    /api/v1/items/:itemId/sessions                         api.v1.ItemSessionApi.create(itemId: ObjectId)
 GET    /testplayer/item/:itemId/render-in-frame     testplayer.controllers.ItemPlayer.renderAsIframe(itemId)
GET    /testplayer/item/:itemId/render              testplayer.controllers.ItemPlayer.renderItem(itemId, printMode: Boolean ?= false, settings : String ?= "")
GET    /testplayer/item/:itemId/run                 testplayer.controllers.ItemPlayer.previewItem(itemId, printMode: Boolean ?= false, settings : String ?= "")
GET    /testplayer/item/:itemId/quiz/:quizId/aggregate           testplayer.controllers.ItemPlayer.renderQuizAsAggregate(quizId, itemId)
GET    /testplayer/item/:itemId/filename           testplayer.controllers.ItemPlayer.getDataFile(itemId, filename)
GET    /testplayer/session/:sessionId/run           testplayer.controllers.ItemPlayer.previewItemBySessionId(sessionId, printMode: Boolean ?= false)
GET    /testplayer/session/:sessionId/instructor    testplayer.controllers.ItemPlayer.renderAsInstructor(sessionId)
GET    /testplayer/session/:sessionId/render        testplayer.controllers.ItemPlayer.renderItemBySessionId(sessionId, printMode: Boolean ?= false)
GET    /testplayer/session/:sessionId/:filename     testplayer.controllers.ItemPlayer.getDataFileBySessionId(sessionId, filename)
GET    /testplayer/javascripts/routes               testplayer.controllers.ItemPlayer.javascriptRoutes
  */
  private def proxyCall(action:Action[AnyContent], authCheck:(RenderRequest[AnyContent])=>Boolean) = RenderAction { request =>
    if(authCheck(request)){
      ApiClient.findByKey(request.ctx.clientId).map(_.orgId) match {
        case Some(orgId) => action(ApiRequest(AuthorizationContext(orgId),request.r,""))
        case None => InternalServerError(JsObject(Seq("message" -> JsString("no client found. this should never occur"))))
      }
    } else Unauthorized(JsObject(Seq("message" -> JsString("you do not have access to the specified content"))))
  }
  def createItemSession(itemId:ObjectId) = proxyCall(
    ItemSessionApi.create(itemId),
    (request) => request.ctx.rc.itemId == RenderConstraints.ALL_ITEMS && request.ctx.rc.itemId == itemId.toString
  )
  def renderItemPlayerAsIframe(itemId:String) = proxyCall(
    ItemPlayer.renderAsIframe(itemId),
    (request) => request.ctx.rc.itemId == RenderConstraints.ALL_ITEMS || request.ctx.rc.itemId == itemId
  )

}
