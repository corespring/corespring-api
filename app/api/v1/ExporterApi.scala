package api.v1

import controllers.auth.{Permission, BaseApi}
import org.bson.types.ObjectId
import models.{Content, Item}

object ExporterApi extends BaseApi{

  def scorm2004( id : ObjectId ) = ApiAction { request =>

    Item.findOneById(id) match {
      case Some(item) => {
        if (Content.isCollectionAuthorized(request.ctx.organization, item.collectionId, Permission.All)){
          //val data = ScormExporter.buildZip(item.id, tokenFromRequest(request).right)
          Ok("Todo..")
        } else {
          BadRequest("You don't have access to this item")
        }
      }
      case _ => NotFound("can't find item")
    }
    Ok("ok")
  }
}
