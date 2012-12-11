package basiclti.controllers

import testplayer.controllers.QtiRenderer
import basiclti.models.{LtiLaunchConfiguration, Assignment}
import play.api.mvc.Action
import models.ItemSession
import controllers.auth.BaseApi
import common.controllers.ItemResources
import org.bson.types.ObjectId

object AssignmentPlayer extends BaseApi with QtiRenderer with ItemResources {

  def run(configId: ObjectId, resultSourcedId: String) = ApiAction {
    request =>

      session(configId, resultSourcedId) match {
        case Left(msg) => BadRequest(msg)
        case Right(session) => {


          getItemXMLByObjectId(session.itemId.toString, request.ctx.organization) match {
            case Some(qti) => {
              val finalXml = prepareQti(qti)
              Ok(
                basiclti.views.html.player(
                  finalXml,
                  session.itemId.toString,
                  session.id.toString,
                  request.token,
                  configId.toString,
                  resultSourcedId
                )

              ).withSession(("access_token", common.mock.MockToken))
            }
            case _ => BadRequest("??")
          }
        }
      }
  }

  private def session(configId: ObjectId, resultSourcedId: String): Either[String, ItemSession] = LtiLaunchConfiguration.findOneById(configId) match {
    case Some(config) => {
      config.assignments.find(_.resultSourcedId == resultSourcedId) match {
        case Some(assignment) => {
          ItemSession.findOneById(assignment.itemSessionId) match {
            case Some(session) => Right(session)
            case _ => Left("can't find session")
          }
        }
        case _ => Left("Can't find assignment")
      }
    }
    case _ => Left("Can't find config")
  }


  def getDataFileForAssignment(configId: ObjectId, resultSourcedId: String, filename: String) = session(configId,resultSourcedId) match {
    case Left(msg) => Action(request => NotFound(msg))
    case Right(session) => getDataFile(session.itemId.toString, filename)
  }

}
