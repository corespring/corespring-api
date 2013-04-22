package player.controllers

import common.controllers.SimpleJsRoutes
import org.bson.types.ObjectId
import play.api.mvc._
import player.controllers.auth.{AllowEverything, Authenticate}
import controllers.auth.{BaseRender, RequestedAccess}


class Session(auth: Authenticate[AnyContent]) extends Controller with SimpleJsRoutes {

  import api.v1.{ItemSessionApi => Api}

  def create(itemId: ObjectId) = auth.OrgAction(
    RequestedAccess(Some(itemId))
  )(Api.create(itemId))

  def read(itemId: ObjectId, sessionId: ObjectId) = auth.OrgAction(
    RequestedAccess(Some(itemId), Some(sessionId))
  )(Api.get(itemId, sessionId))

  def update(itemId: ObjectId, sessionId: ObjectId, action: Option[String] = None) = auth.OrgAction(
    RequestedAccess(Some(itemId), Some(sessionId))
  )(Api.update(itemId, sessionId, action))

  def aggregate(quizId: ObjectId, itemId: ObjectId) = auth.OrgAction(
    RequestedAccess(Some(itemId))
  )(Api.aggregate(quizId, itemId))

  def jsRoutes = Action {
    implicit request =>
      import routes.javascript.{Session => JsSession}
      val jsRoutes = List(
        JsSession.create,
        JsSession.read,
        JsSession.aggregate,
        JsSession.update
      )
      Ok(createSimpleRoutes("PlayerRoutes", jsRoutes: _*))
  }
}

//object Session extends Session(AllowEverything)
object Session extends Session(BaseRender)
/*
import controllers.auth._
import models.Organization
import com.mongodb.casbah.Imports._
import com.novus.salat._
import models.mongoContext._
import scala.Some
import testplayer.controllers.ItemPlayer
import models.itemSession.ItemSession

import play.api.libs.json.{JsString, JsObject}


class Session(auth: Authenticate) extends Controller {

  def create(itemId:ObjectId) = auth.ApiAction(RequestedAccess(Some(itemId))) { request =>
    api.v1.ItemSessionApi.create(itemId)(request)
  }

  def read(itemId: ObjectId, sessionId: ObjectId) = auth.ApiAction(RequestedAccess(Some(itemId),Some(sessionId))){
    request =>
      api.v1.ItemSessionApi.get(itemId, sessionId)(request)
  }

  def update(sessionId:ObjectId,action:Option[String]) = auth.ApiAction(RequestedAccess(None,Some(sessionId))) {request =>
    ItemSession.findOneById(sessionId) match {
      case Some(session) => api.v1.ItemSessionApi.update(session.itemId,sessionId,action)(request)
      case None => InternalServerError(JsObject(Seq("message" -> JsString("no session found even after authenticated"))))
    }
  }

  def aggregate(quizId: ObjectId, itemId: ObjectId) = auth.ApiAction(RequestedAccess(Some(itemId),None,Some(quizId))) { request =>
    api.v1.ItemSessionApi.aggregate(quizId,itemId)(request)
  }

  def jsRoutes = Action {
    implicit request =>
//
//      import routes.javascript.{Session => JsSession}
//
//      Ok(
//        play.api.Routes.javascriptRouter("TestPlayerRoutes")(
//          JsSession.create,
//          JsSession.read,
//          JsSession.aggregate,
//          JsSession.update
//        )
//      ).as("text/javascript")
      Ok
  }
}

object Session extends Session(BaseRender)
*/
