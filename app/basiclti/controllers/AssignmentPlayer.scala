package basiclti.controllers

import basiclti.models.LtiQuiz
import common.controllers.AssetResource
import models.itemSession.ItemSession
import org.bson.types.ObjectId
import play.api.mvc.Action
import player.accessControl.auth.CheckPlayerSession
import player.accessControl.models.RequestedAccess
import player.controllers.Views

object AssignmentPlayer extends Views(CheckPlayerSession) with AssetResource {

  def run(configId: ObjectId, resultSourcedId: String) = {
    session(configId, resultSourcedId) match {
      case Left(msg) => Action(r => BadRequest(msg))
      case Right(session) => renderItem(session.itemId.toString, previewEnabled = false, mode = RequestedAccess.ADMINISTER_MODE)
    }
  }

  private def session(configId: ObjectId, resultSourcedId: String): Either[String, ItemSession] = LtiQuiz.findOneById(configId) match {
    case Some(config) => {

      config.participants.find(_.resultSourcedId == resultSourcedId) match {
        case Some(p) => {
          ItemSession.findOneById(p.itemSession) match {
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
      getDataFile(session.itemId.toString, filename)
    }
  }

}
