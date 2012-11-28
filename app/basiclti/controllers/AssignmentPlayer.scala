package basiclti.controllers

import testplayer.controllers.QtiRenderer
import basiclti.models.Assignment
import play.api.mvc.Action
import models.ItemSession
import controllers.auth.BaseApi
import common.controllers.ItemResources
import org.bson.types.ObjectId

object AssignmentPlayer extends BaseApi with QtiRenderer with ItemResources {

  def runByAssignmentId(assignmentId: ObjectId) = ApiAction {
    request =>

      Assignment.findOneById(assignmentId) match {
        case Some(assignment) => {
          ItemSession.findOneById(assignment.itemSessionId) match {
            case Some(session) => {
              getItemXMLByObjectId(session.itemId.toString, request.ctx.organization) match {
                case Some(qti) => Ok(
                  basiclti.views.html.player(
                    prepareQti(qti),
                    session.itemId.toString,
                    session.id.toString,
                    request.token,
                    assignment.id.toString)

                ).withSession(("access_token", common.mock.MockToken))

                case _ => NotFound("can't find item with id: " + session.itemId.toString)
              }
            }
            case _ => NotFound("Can't find Item by Session " + assignmentId.toString)
          }
        }
        case _ => NotFound("Can't find assigmnent with that id")
      }
  }

  def getDataFileByAssignmentId(assignmentId: ObjectId, filename: String) =
    Assignment.findOneById(assignmentId) match {
      case Some(assignment) => {
        ItemSession.findOneById(assignment.itemSessionId) match {
          case Some(session) => {
            getDataFile(session.itemId.toString, filename)
          }
          case _ => Action(request => NotFound("Can't find item session"))
        }
      }
      case _ => Action(request => NotFound("can't find assignment by id: " + assignmentId))
    }

}
