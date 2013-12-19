package api.v1

import basiclti.export.CCExporter
import com.mongodb.casbah.commons.MongoDBObject
import common.controllers.utils.BaseUrl
import common.encryption._
import controllers.auth.{ BaseApi }
import org.bson.types.ObjectId
import org.corespring.platform.data.mongo.models.VersionedId
import play.api.libs.iteratee.Enumerator
import play.api.libs.json.Json
import play.api.mvc._
import scala.Some
import scorm.utils.ScormExporter
import org.corespring.platform.core.models.versioning.VersionedIdImplicits
import org.corespring.platform.core.models.item.{ Item, Content }
import org.corespring.platform.core.models.auth.Permission
import org.corespring.common.encryption.{ Crypto, AESCrypto }
import org.corespring.player.accessControl.models.RenderOptions
import org.corespring.platform.core.services.item.{ ItemServiceImpl, ItemService }

class ExporterApi(encrypter: Crypto, service: ItemService) extends BaseApi {

  val OctetStream: String = "application/octet-stream"

  /**
   * Build a multi item scorm .zip
   * @param ids - comma delimited list of ids
   */
  def multiItemScorm2004(ids: String) = ApiActionRead {
    request =>

      logger.debug("Encrypt for org: " + request.ctx.org.map(_.name).getOrElse("??"))
      val orgEncrypter = new OrgEncrypter(request.ctx.organization, encrypter)
      val options: RenderOptions = RenderOptions.ANYTHING
      val maybeResult = orgEncrypter.encrypt(Json.toJson(options).toString())

      maybeResult match {
        case Some(EncryptionSuccess(clientId, data, _)) => {
          val generatorFn: List[Item] => Array[Byte] = ScormExporter.makeMultiScormPackage(_, BaseUrl(request), clientId, data)
          binaryResultFromIds(ids, request.ctx.organization, generatorFn)
        }
        case Some(EncryptionFailure(msg, t)) => {
          logger.debug("multiItemScorm encryption error: " + msg + " " + t.getMessage)
          BadRequest("An error occurred")
        }
        case _ => BadRequest("Unable to create export package")
      }
  }

  def multiItemLti(ids: String) = ApiActionRead {
    request =>
      binaryResultFromIds(ids, request.ctx.organization, (i) => CCExporter.packageItems(i.map(_.id.toString), BaseUrl(request)))
  }

  private def binaryResultFromIds(ids: String, orgId: ObjectId, itemsToByteArray: (List[Item] => Array[Byte])): Result = {
    val validIds = validObjectIds(ids)
    val items = service.findMultiple(validIds).toList
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

  private def validObjectIds(ids: String): List[VersionedId[ObjectId]] = {
    import VersionedIdImplicits.Binders._
    ids.split(",").toList.map(stringToVersionedId).flatten
  }

  private def access(orgId: ObjectId)(item: Item): Boolean = Content.isCollectionAuthorized(orgId, item.collectionId, Permission.Read)

  private def Binary(data: Array[Byte], length: Option[Long] = None, contentType: String = "application/octet-stream") = {
    val e = Enumerator(data)
    SimpleResult(header = ResponseHeader(
      OK,
      Map(CONTENT_TYPE -> contentType) ++ length.map(length =>
        Map(CONTENT_LENGTH -> (length.toString))).getOrElse(Map.empty)),
      body = e)
  }
}

object ExporterApi extends ExporterApi(AESCrypto, ItemServiceImpl)
