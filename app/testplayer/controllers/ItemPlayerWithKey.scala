package testplayer.controllers

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
import models.item.Content
import api.ApiError
import controllers.{Utils, InternalError}

object ItemPlayerWithKey extends BaseApi with BaseRender{

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
  def renderOptions(itemId:String,sessionId:String,mode:String) = RenderAction {implicit request =>
    val finalMode = if (request.ctx.options.mode == "*") mode else request.ctx.options.mode
    finalMode match {
      case "preview" => finalId(itemId,request.ctx.options.itemId,"item") match {
        case Right(iid) => previewMode(iid)
        case Left(error) => BadRequest(Json.toJson(ApiError.ItemPlayer(error.clientOutput)))
      }
      case "render" => finalId(sessionId,request.ctx.options.sessionId,"session") match {
        case Right(iid) => renderMode(iid)
        case Left(error) => BadRequest(Json.toJson(ApiError.ItemPlayer(error.clientOutput)))
      }
      case "administer" => finalId(itemId,request.ctx.options.itemId,"item") match {
        case Right(iid) => finalId(sessionId,request.ctx.options.sessionId, "session") match {
          case Right(sid) => administerMode(iid,Some(sid))
          case Left(error) => administerMode(iid,None)
        }
        case Left(error) => BadRequest(Json.toJson(ApiError.ItemPlayer(error.clientOutput)))
      }
      case "aggregate" => finalId(itemId,request.ctx.options.itemId, "item") match {
        case Right(iid) => request.ctx.options.assessmentId.flatMap(Utils.toObjectId(_)) match {
          case Some(assessmentId) => aggregateMode(iid,assessmentId)
          case None => BadRequest(Json.toJson(ApiError.ItemPlayer(Some("could not parse assessment id"))))
        }
        case Left(error) => BadRequest(Json.toJson(ApiError.ItemPlayer(error.clientOutput)))
      }
      case _ => BadRequest(Json.toJson(ApiError.ItemPlayer(Some("invalid item player mode"))))
    }
  }

  /**
   * compares id with compareId. if compareId is "*" then take the value of id. otherwise, use compareId.
   * @param id
   * @param compareId
   * @param idType this is just used for error outputs
   * @return
   */
  private def finalId(id:String,compareId:Option[String],idType:String):Either[InternalError,ObjectId] = compareId match {
    case Some(ctxId) => if (ctxId == "*"){
      if(id != ""){
        Utils.toObjectId(id).map(Right(_)).getOrElse(Left(InternalError("no valid "+idType+" id found", addMessageToClientOutput = true)))
      } else Left(InternalError("no valid item id found",addMessageToClientOutput = true))
    } else Utils.toObjectId(ctxId).map(Right(_)).getOrElse(Left(InternalError("no valid "+idType+" id found", addMessageToClientOutput = true)))
    case None => Left(InternalError("no valid item id found",addMessageToClientOutput = true))
  }
  //TODO: move item player stuff to here (remember to include content authorization)
  private def previewMode(itemId:ObjectId)(implicit request:RenderRequest[AnyContent]):Result = {
    ItemPlayer.previewItem(itemId.toString)(request)
  }
  private def renderMode(sessionId:ObjectId)(implicit request:RenderRequest[AnyContent]):Result = {
    request.ctx.options.role.getOrElse("student") match {
      case "student" => ItemPlayer.renderItemBySessionId(sessionId.toString)(request)
      case "instructor" => ItemPlayer.renderAsInstructor(sessionId.toString)(request)
    }
  }
  private def administerMode(itemId:ObjectId, optsessionId:Option[ObjectId])(implicit request:RenderRequest[AnyContent]):Result = {
    optsessionId match {
      case Some(sessionId) => ItemPlayer.renderItemBySessionId(sessionId.toString)(request)
      case None => ItemPlayer.renderItem(itemId.toString)(request)
    }
  }
  private def aggregateMode(itemId:ObjectId, assessmentId:ObjectId)(implicit request:RenderRequest[AnyContent]):Result = {
    ItemPlayer.renderQuizAsAggregate(assessmentId.toString,itemId.toString)(request)
  }
}

