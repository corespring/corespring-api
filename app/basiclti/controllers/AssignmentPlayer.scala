package basiclti.controllers

import basiclti.models.LtiQuiz
import common.controllers.AssetResource
import org.bson.types.ObjectId
import play.api.mvc.Action
import player.accessControl.auth.{CheckSessionAccess, CheckSession}
import player.accessControl.models.RequestedAccess
import player.controllers.Views
import org.corespring.platform.core.models.item.service.ItemServiceImpl
import player.views.models.PlayerParams
import org.corespring.platform.core.models.itemSession.{DefaultItemSession, ItemSession}
import org.corespring.platform.core.models.versioning.VersionedIdImplicits
import org.corespring.platform.core.models.quiz.basic.Quiz

object AssignmentPlayer extends Views(CheckSessionAccess, ItemServiceImpl, Quiz) with AssetResource {

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
      getDataFile(versionedIdToString(session.itemId), filename)
    }
  }

}
