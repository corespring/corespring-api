package api.v1

import controllers.auth.{Permission, BaseApi}
import org.bson.types.ObjectId
import models.{Content, Item}
import scorm.utils.ScormExporter
import play.api.libs.iteratee.Enumerator
import play.api.mvc.{ResponseHeader, SimpleResult}
import common.mock._
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.BasicDBObject

object ExporterApi extends BaseApi {


  val OctetStream: String = "application/octet-stream"

  def scorm2004(id: ObjectId) = ApiAction {
    request =>
      Item.findOneById(id) match {
        case Some(item) => {
          if (Content.isCollectionAuthorized(request.ctx.organization, item.collectionId, Permission.All)) {
            //TODO: Need to associate a non expiring token with this users' org and pass it in here.
            val data = ScormExporter.makeMultiScormPackage(List(item), MockToken)
            Binary(data, None, OctetStream)
          } else {
            BadRequest("You don't have access to this item")
          }
        }
        case _ => NotFound("can't find item")
      }
  }

  def multiItemScorm2004(ids: String) = ApiAction {
    request =>

      val validIds = ids.split(",").toList.map {
        rawId =>
          try {
            Some(new ObjectId(rawId))
          } catch {
            case e: Throwable => None
          }
      }.flatten

      def access(orgId: ObjectId)(item: Item): Boolean = Content.isCollectionAuthorized(orgId, item.collectionId, Permission.All)

      val items = Item.find(MongoDBObject("_id" -> MongoDBObject("$in" -> validIds)))
        .toList
        .filter(access(request.ctx.organization))

      val data = ScormExporter.makeMultiScormPackage(items, MockToken)
      Binary(data, None, OctetStream)
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
