package basiclti.controllers

import basiclti.models
import basiclti.models.LtiQuiz
import org.bson.types.ObjectId
import play.api.libs.json.Json._
import play.api.mvc._
import player.controllers.auth.Authenticate
import player.rendering.PlayerCookieReader
import scala.Left
import scala.Right
import scala.Some


class LtiQuizzes(auth:Authenticate[ObjectId,AnyContent,OrgRequest[AnyContent]]) extends Controller {

  def get(id: ObjectId) = auth.OrgAction(id){
    request =>

      models.LtiQuiz.findOneById(id) match {
        case Some(c) => Ok(toJson(c))
        case _ => NotFound("Can't find launch config with that id")
      }
  }

  def update(id: ObjectId) = auth.OrgAction(id) {
    request =>

      quiz(request) match {
        case Some(cfg) if (id != cfg.id) => BadRequest("the json id doesn't match the url id")
        case Some(cfg) => {
          models.LtiQuiz.update(cfg, request.orgId) match {
            case Left(e) => BadRequest("Error updating")
            case Right(updatedConfig) => Ok(toJson(updatedConfig))
          }
        }
        case _ => BadRequest("Invalid json provided")
      }
  }

  private def quiz(request: Request[AnyContent]): Option[LtiQuiz] = {
    request.body.asJson match {
      case Some(json) => {
        try {
          val out = json.asOpt[LtiQuiz]
          out
        }
        catch {
          case e: Throwable => {
            play.Logger.warn(e.getMessage)
            None
          }
        }
      }
      case _ => None
    }
  }
}

object LtiCookieKeys{
  val QUIZ_ID = "lti.quiz.id"
}

case class OrgRequest[A](orgId:ObjectId, r:Request[A]) extends WrappedRequest[A](r)

object Mock extends Authenticate[ObjectId,AnyContent,OrgRequest[AnyContent]]  with PlayerCookieReader{

  import play.api.mvc.Results._

  def OrgAction(accessId: ObjectId)(block: (OrgRequest[AnyContent]) => Result): Action[AnyContent] =
    OrgAction(play.api.mvc.BodyParsers.parse.anyContent)(accessId)(block)

  def OrgAction(p: BodyParser[AnyContent])(access: ObjectId)(block: (OrgRequest[AnyContent]) => Result): Action[AnyContent] = {
    Action{
      request =>
        request.session.get(LtiCookieKeys.QUIZ_ID) match {
          case Some(id) => {
            if(access == id){
              orgIdFromCookie(request) match {
                case Some(orgId) => block( new OrgRequest(new ObjectId(orgId), request))
                case _ => BadRequest("no org id")
              }
            }else{
              BadRequest("")
            }
          }
          case _ => BadRequest("")
        }
    }
  }
}


object LtiQuizzes extends LtiQuizzes(Mock)
