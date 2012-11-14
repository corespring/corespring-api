package api.v1

import controllers.auth.{Permission, BaseApi}
import org.bson.types.ObjectId
import models.{Content, Item}
import scorm.utils.ScormExporter
import play.api.libs.iteratee.Enumerator
import play.api.mvc.{ResponseHeader, SimpleResult}

object ExporterApi extends BaseApi {

  def scorm2004(id: ObjectId) = ApiAction {
    request =>
      Item.findOneById(id) match {
        case Some(item) => {
          if (Content.isCollectionAuthorized(request.ctx.organization, item.collectionId, Permission.All)) {
            val data = ScormExporter.makeScormPackage(item.id.toString, request.token)
            Binary(data, None, "application/octet-stream")
          } else {
            BadRequest("You don't have access to this item")
          }
        }
        case _ => NotFound("can't find item")
      }
  }

  private def Binary(data: Array[Byte], length: Option[Long] = None, contentType: String = "application/octet-stream") = {

    val e = Enumerator(data)

    SimpleResult[Array[Byte]](header = ResponseHeader(
      OK,
      Map(CONTENT_TYPE -> contentType) ++ length.map(length =>
        Map(CONTENT_LENGTH -> (length.toString))).getOrElse(Map.empty)),
      body = e)

  }
}
