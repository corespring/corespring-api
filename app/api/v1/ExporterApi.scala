package api.v1

import controllers.auth.{Permission, BaseApi}
import org.bson.types.ObjectId
import scorm.utils.ScormExporter
import play.api.libs.iteratee.Enumerator
import play.api.mvc._
import common.mock._
import com.mongodb.casbah.commons.MongoDBObject
import basiclti.export.CCExporter
import scala.Some
import play.api.mvc.SimpleResult
import play.api.mvc.ResponseHeader
import common.controllers.utils.BaseUrl
import models.item.{Content, Item}

object ExporterApi extends BaseApi {

  val OctetStream: String = "application/octet-stream"


  /** Build a multi item scorm .zip
   * @param ids - comma delimited list of ids
   */
  def multiItemScorm2004(ids: String) = ApiActionRead{ request =>
    binaryResultFromIds(ids, request.ctx.organization, ScormExporter.makeMultiScormPackage(_,MockToken,BaseUrl(request)))
  }


  def multiItemLti(ids: String) = ApiActionRead { request =>
    binaryResultFromIds( ids, request.ctx.organization, (i) => CCExporter.packageItems(i.map(_.id.toString), BaseUrl(request)) )
  }

  private def binaryResultFromIds(ids:String, orgId : ObjectId, itemsToByteArray : (List[Item] => Array[Byte])) : Result = {
    val validIds = validObjectIds(ids)
    val items = Item.find(MongoDBObject("_id" -> MongoDBObject("$in" -> validIds))).toList
    items match {
      case List() => NotFound("No items found")
      case _ => {
        items.filter(access(orgId)) match {
          case List() => Unauthorized("You don't have access to these items")
          case _ => Binary(itemsToByteArray(items), None, OctetStream)
        }
      }
    }
  }

  private def validObjectIds(ids:String) : List[ObjectId] = {
    ids.split(",").toList.map {
      rawId =>
        try {
          Some(new ObjectId(rawId))
        } catch {
          case e: Throwable => None
        }
    }.flatten
  }

  private def access(orgId: ObjectId)(item: Item): Boolean = Content.isCollectionAuthorized(orgId, item.collectionId, Permission.Read)

  private def Binary(data: Array[Byte], length: Option[Long] = None, contentType: String = "application/octet-stream") = {
    val e = Enumerator(data)
    SimpleResult[Array[Byte]](header = ResponseHeader(
      OK,
      Map(CONTENT_TYPE -> contentType) ++ length.map(length =>
        Map(CONTENT_LENGTH -> (length.toString))).getOrElse(Map.empty)),
      body = e)
  }
}
