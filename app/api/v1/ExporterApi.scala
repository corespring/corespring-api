package api.v1

import basiclti.export.CCExporter
import com.mongodb.casbah.commons.MongoDBObject
import common.controllers.utils.BaseUrl
import common.encryption._
import controllers.auth.{Permission, BaseApi}
import models.item.{Content, Item}
import org.bson.types.ObjectId
import play.api.libs.iteratee.Enumerator
import play.api.libs.json.Json
import play.api.mvc.ResponseHeader
import play.api.mvc.SimpleResult
import play.api.mvc._
import player.accessControl.models.RenderOptions
import scala.Some
import scorm.utils.ScormExporter

class ExporterApi(encrypter:Crypto) extends BaseApi {

  val OctetStream: String = "application/octet-stream"


  /** Build a multi item scorm .zip
   * @param ids - comma delimited list of ids
   */
  def multiItemScorm2004(ids: String) = ApiActionRead{ request =>

    val orgEncrypter = new OrgEncrypter(request.ctx.organization, encrypter)
    val options : RenderOptions = RenderOptions.ANYTHING
    orgEncrypter.encrypt(Json.toJson(options).toString()) match {
      case Some(EncryptionSuccess(clientId,data, None)) => {
        val generatorFn : List[Item] => Array[Byte] = ScormExporter.makeMultiScormPackage(_,BaseUrl(request), clientId, data)
        binaryResultFromIds(ids, request.ctx.organization, generatorFn )
      }
      case _ => BadRequest("Unable to create export package")
    }
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

object ExporterApi extends ExporterApi(AESCrypto)
