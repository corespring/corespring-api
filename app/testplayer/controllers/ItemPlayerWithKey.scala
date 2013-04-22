package testplayer.controllers

import controllers.auth._
import play.api.libs.json.{Json}
import org.bson.types.ObjectId
import play.api.mvc.{SimpleResult, Result, AnyContent}
import api.ApiError
import controllers.{Utils}
import controllers.auth.BaseRender._
import scala.Left
import controllers.InternalError
import scala.Right
import scala.Some

object ItemPlayerWithKey extends BaseApi{

  def renderPlayer(itemId:String,sessionId:String,mode:String) = BaseRender.RenderAction {implicit request =>
    val finalMode = if (request.ctx.options.mode == "*") mode else request.ctx.options.mode
    finalMode match {
      case "preview" => finalId(itemId,request.ctx.options.itemId,"item") match {
        case Right(iid) => previewMode(iid).asInstanceOf[SimpleResult[_]].withSession(RendererHeader -> request.reencrypt)
        case Left(error) => BadRequest(Json.toJson(ApiError.ItemPlayer(error.clientOutput)))
      }
      case "render" => finalId(sessionId,request.ctx.options.sessionId,"session") match {
        case Right(iid) => renderMode(iid).asInstanceOf[SimpleResult[_]].withSession(RendererHeader -> request.reencrypt)
        case Left(error) => BadRequest(Json.toJson(ApiError.ItemPlayer(error.clientOutput)))
      }
      case "administer" => finalId(itemId,request.ctx.options.itemId,"item") match {
        case Right(iid) => finalId(sessionId,request.ctx.options.sessionId, "session") match {
          case Right(sid) => administerMode(iid,Some(sid)).asInstanceOf[SimpleResult[_]].withSession(RendererHeader -> request.reencrypt)
          case Left(error) => administerMode(iid,None).asInstanceOf[SimpleResult[_]].withSession(RendererHeader -> request.reencrypt)
        }
        case Left(error) => BadRequest(Json.toJson(ApiError.ItemPlayer(error.clientOutput)))
      }
      case "aggregate" => finalId(itemId,request.ctx.options.itemId, "item") match {
        case Right(iid) => request.ctx.options.assessmentId.flatMap(Utils.toObjectId(_)) match {
          case Some(assessmentId) => aggregateMode(iid,assessmentId).asInstanceOf[SimpleResult[_]].withSession(RendererHeader -> request.reencrypt)
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

