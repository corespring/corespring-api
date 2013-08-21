package basiclti.controllers

import basiclti.models.LtiQuiz
import common.controllers.AssetResource
import org.bson.types.ObjectId
import org.corespring.platform.core.models.itemSession.{DefaultItemSession, ItemSession}
import org.corespring.platform.core.models.versioning.VersionedIdImplicits
import org.corespring.platform.core.services.quiz.basic.QuizService
import org.corespring.player.accessControl.auth.CheckSessionAccess
import org.corespring.player.accessControl.models.RequestedAccess
import play.api.mvc.Action
import player.controllers.Views
import player.views.models.PlayerParams
import org.corespring.platform.core.services.item.ItemServiceImpl

object AssignmentPlayer extends Views(CheckSessionAccess, ItemServiceImpl, QuizService) with AssetResource {

  def run(configId: ObjectId, resultSourcedId: String) = {
    session(configId, resultSourcedId) match {
      case Left(msg) => Action(r => BadRequest(msg))
      case Right(session) => {
        val p = RenderParams(
          session.itemId,
          sessionId = Some(session.id),
          sessionMode = RequestedAccess.Mode.Administer,
          templateFn = (p:PlayerParams) => basiclti.views.html.LtiPlayer(p, configId.toString, resultSourcedId))
        renderItem(p)
      }
    }
  }

  private def session(configId: ObjectId, resultSourcedId: String): Either[String, ItemSession] = LtiQuiz.findOneById(configId) match {
    case Some(config) => {

      config.participants.find(_.resultSourcedId == resultSourcedId) match {
        case Some(p) => {
          DefaultItemSession.findOneById(p.itemSession) match {
            case Some(session) => {
              config.orgId match {
                case Some(id) => Right(session)
                case _ => Left("can't find org id")
              }
            }
            case _ => Left("can't find session")
          }
        }
        case _ => Left("Can't find assignment")
      }
    }
    case _ => Left("Can't find config")
  }


  def getDataFileForAssignment(configId: ObjectId, resultSourcedId: String, filename: String) = session(configId, resultSourcedId) match {
    case Left(msg) => Action(request => NotFound(msg))
    case Right(session) => {

      import VersionedIdImplicits.Binders._
      getDataFile(session.itemId.toString(), filename)
    }
  }

}
