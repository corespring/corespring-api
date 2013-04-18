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
import models.itemSession.ItemSessionSettings

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
  */
  private def proxyCall(authCheck:(RenderRequest[AnyContent])=>Boolean, action:Action[AnyContent]) = RenderAction { request =>
    if(authCheck(request)){
      ApiClient.findByKey(request.ctx.clientId).map(_.orgId) match {
        case Some(orgId) => action(ApiRequest(AuthorizationContext(orgId),request.r,""))
        case None => InternalServerError(JsObject(Seq("message" -> JsString("no client found. this should never occur"))))
      }
    } else Unauthorized(JsObject(Seq("message" -> JsString("you do not have access to the specified content"))))
  }
  def renderAsIframe(itemId:String) = proxyCall(
    (request) => RenderConstraints.authCheck(RenderConstraints(Some(itemId.toString), expires = request.ctx.rc.expires),request.ctx.rc),
    ItemPlayer.renderAsIframe(itemId)
  )
  def renderItem(itemId:String, printMode:Boolean, settings: String) = proxyCall(
    (request) => RenderConstraints.authCheck(
      RenderConstraints( Some(itemId.toString),
        settings = if (settings == "") None else Some(Json.fromJson[ItemSessionSettings](Json.parse(settings))),
        expires = request.ctx.rc.expires),
      request.ctx.rc
    ),
    ItemPlayer.renderItem(itemId, printMode, settings)
  )
  def previewItem(itemId:String, printMode:Boolean, settings: String) = proxyCall(
    (request) => RenderConstraints.authCheck(
      RenderConstraints(Some(itemId.toString),
        settings = if (settings == "") None else Some(Json.fromJson[ItemSessionSettings](Json.parse(settings))),
        expires = request.ctx.rc.expires),
      request.ctx.rc
    ),
    ItemPlayer.previewItem(itemId, printMode, settings)
  )
  def renderQuizAsAggregate(quizId:String, itemId:String) = proxyCall(
    (request) => RenderConstraints.authCheck(RenderConstraints(Some(itemId),assessmentId = Some(quizId), expires = request.ctx.rc.expires), request.ctx.rc),
    ItemPlayer.renderQuizAsAggregate(quizId,itemId)
  )
  def getDataFile(itemId:String, filename:String) = proxyCall(
    (request) => RenderConstraints.authCheck(RenderConstraints(Some(itemId.toString), expires = request.ctx.rc.expires),request.ctx.rc),
    ItemPlayer.getDataFile(itemId, filename)
  )
  def previewItemBySessionId(sessionId: String, printMode:Boolean) = proxyCall(
    (request) => RenderConstraints.authCheck(RenderConstraints(itemSessionId = Some(sessionId.toString), expires = request.ctx.rc.expires),request.ctx.rc),
    ItemPlayer.previewItemBySessionId(sessionId, printMode)
  )
  def renderAsInstructor(sessionId:String) = proxyCall(
    (request) => RenderConstraints.authCheck(RenderConstraints(itemSessionId = Some(sessionId), expires = request.ctx.rc.expires),request.ctx.rc),
    ItemPlayer.renderAsInstructor(sessionId)
  )
  def renderItemBySessionId(sessionId:String,printMode:Boolean) = proxyCall(
    (request) => RenderConstraints.authCheck(RenderConstraints(itemSessionId = Some(sessionId), expires = request.ctx.rc.expires),request.ctx.rc),
    ItemPlayer.renderItemBySessionId(sessionId,printMode)
  )
  def getDataFileBySessionId(sessionId:String, filename:String) = proxyCall(
    (request) => RenderConstraints.authCheck(RenderConstraints(itemSessionId = Some(sessionId), expires = request.ctx.rc.expires),request.ctx.rc),
    ItemPlayer.getDataFileBySessionId(sessionId,filename)
  )
}
