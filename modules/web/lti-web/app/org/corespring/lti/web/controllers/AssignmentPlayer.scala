package org.corespring.lti.web.controllers

import org.bson.types.ObjectId
import org.corespring.lti.models.LtiAssessment
import org.corespring.platform.core.controllers.AssetResource
import org.corespring.platform.core.models.itemSession.{ DefaultItemSession, ItemSession }
import org.corespring.platform.core.services.assessment.basic.AssessmentService
import org.corespring.platform.core.services.item.ItemServiceWired
import org.corespring.player.accessControl.auth.CheckSessionAccess
import org.corespring.player.accessControl.models.RequestedAccess
import org.corespring.player.v1.controllers.Views
import org.corespring.player.v1.views.models.PlayerParams
import play.api.mvc.Action

object AssignmentPlayer extends Views(CheckSessionAccess, ItemServiceWired, AssessmentService) with AssetResource {

  def run(configId: ObjectId, resultSourcedId: String) = {
    session(configId, resultSourcedId) match {
      case Left(msg) => Action(BadRequest(msg))
      case Right(session) => {
        val p = RenderParams(
          session.itemId,
          sessionId = Some(session.id),
          sessionMode = RequestedAccess.Mode.Administer,
          templateFn = (p: PlayerParams) => org.corespring.lti.web.views.html.LtiPlayer(p, configId.toString, resultSourcedId))
        renderItem(p)
      }
    }
  }

  private def session(configId: ObjectId, resultSourcedId: String): Either[String, ItemSession] = LtiAssessment.findOneById(configId) match {
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

      getDataFile(session.itemId.toString(), filename)
    }
  }

}
