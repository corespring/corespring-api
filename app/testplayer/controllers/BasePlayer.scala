package testplayer.controllers

import org.bson.types.ObjectId
import controllers.auth.BaseApi
import common.controllers.ItemResources
import play.api.mvc.{Result, AnyContent}
import models.itemSession.ItemSession


trait BasePlayer extends BaseApi with QtiRenderer with ItemResources{

  def OkRunPlayer(xml:String,itemId:String,sessionId:String,token:String) : Result

  def runBySessionId(sessionId: ObjectId) = ApiAction {
    request =>

      ItemSession.findOneById(sessionId) match {
        case Some(session) => {
          getItemXMLByObjectId(session.itemId.toString, request.ctx.organization) match {
            case Some(qti) => OkRunPlayer(prepareQti(qti), session.itemId.toString, sessionId.toString, request.token)
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
        case Some(qti) => OkRunPlayer(prepareQti(qti), itemId.toString, "", request.token)
        case _ => NotFound("can't find item with id: " + itemId.toString)
      }
  }

}

