package player.controllers

import play.api.mvc._
import play.api.mvc.BodyParsers
import org.bson.types.ObjectId
import controllers.auth.{AuthorizationContext, ApiRequest}
import models.Organization
import com.mongodb.casbah.commons.MongoDBObject


trait Authenticate[A] {
  def OrgAction(p: BodyParser[A])(block: ApiRequest[A] => Result): Action[A]

  def OrgAction(block: ApiRequest[A] => Result): Action[A]
}

object NullAuth extends Authenticate[AnyContent] {

  def OrgAction(block: ApiRequest[AnyContent] => Result): Action[AnyContent] = OrgAction(BodyParsers.parse.anyContent)(block)

  def OrgAction(p: BodyParser[AnyContent])(block: ApiRequest[AnyContent] => Result): Action[AnyContent] = {
    Action(p) {
      request =>

        Organization.findOne(MongoDBObject("name" -> "Corespring Organization")) match {
          case Some(org) => {
            val context = AuthorizationContext(org.id, None, isSSLogin = false)
            block(ApiRequest(context, request, "token"))
          }
          case _ => throw new RuntimeException("Can't find Corespring Organization")
        }
    }
  }
}

class Session(auth: Authenticate[AnyContent]) extends Controller {

  def create() = Action {
    request => Ok("todo")
  }

  def read(itemId: ObjectId, sessionId: ObjectId) = auth.OrgAction( api.v1.ItemSessionApi.get(itemId, sessionId) )

  def update(id: ObjectId) = Action {
    request => Ok("todo")
  }

  def aggregate(quizId: ObjectId, itemId: ObjectId) = Action {
    request => Ok("todo")
  }

  def jsRoutes = Action {
    implicit request =>

      import routes.javascript.{Session => JsSession}

      Ok(
        play.api.Routes.javascriptRouter("TestPlayerRoutes")(
          JsSession.create,
          JsSession.read,
          JsSession.aggregate,
          JsSession.update
        )
      ).as("text/javascript")
  }
}

object Session extends Session(NullAuth)
