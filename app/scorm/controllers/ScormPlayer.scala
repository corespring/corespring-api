package scorm.controllers

import controllers.auth.BaseApi
import org.bson.types.ObjectId
import common.controllers.ItemResources
import testplayer.controllers.QtiRenderer
import models.ItemSession

object ScormPlayer extends BaseApi with ItemResources with QtiRenderer {

  def runBySessionId(sessionId: ObjectId) = ApiAction {
    request =>

      ItemSession.findOneById(sessionId) match {
        case Some(session) => {
          getItemXMLByObjectId(session.itemId.toString, request.ctx.organization) match {
            case Some(qti) => Ok(scorm.views.html.run(prepareQti(qti), session.itemId.toString, sessionId.toString, request.token))
            case _ => NotFound("can't find item with id: " + session.itemId.toString)
          }
        }
        case _ => NotFound("Can't find Item by Session " + sessionId.toString)
      }
  }

  def getDataFileBySessionId(sessionId: ObjectId, filename: String) = {
    ItemSession.findOneById(sessionId) match {
      case Some(session) => getDataFile(session.itemId.toString, filename: String)
      case _ => ApiAction(request => NotFound(filename))
    }
  }

  def runByItemId(itemId: ObjectId) = ApiAction {
    request =>
      getItemXMLByObjectId(itemId.toString, request.ctx.organization) match {
        case Some(qti) => Ok(scorm.views.html.run(prepareQti(qti), itemId.toString, "", request.token))
        case _ => NotFound("can't find item with id: " + itemId.toString)
      }
  }
}
