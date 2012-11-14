package scorm.controllers

import controllers.auth.BaseApi
import org.bson.types.ObjectId
import common.controllers.ItemResources
import testplayer.controllers.QtiRenderer

object ScormPlayer extends BaseApi with ItemResources with QtiRenderer {

  def run(itemId: ObjectId) = ApiAction {
    request =>

      getItemXMLByObjectId(itemId.toString, request.ctx.organization) match {
        case Some(qti) => {
          val itemBody = prepareQti(qti)
          Ok(scorm.views.html.run(itemBody))
        }
        case _ => NotFound("can't find item with id: " + itemId.toString)
      }
  }
}
