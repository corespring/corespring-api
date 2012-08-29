package api.v1

import controllers.auth.BaseApi
import com.typesafe.config.ConfigFactory
import play.api.mvc.Action
import controllers.S3Service
import common.models.SupportingMaterialFile
import models.{Item, ItemFile}
import org.bson.types.ObjectId
import play.api.libs.json.Json._

object MaterialsApi extends BaseApi {

  private final val AMAZON_ASSETS_BUCKET: String = ConfigFactory.load().getString("AMAZON_ASSETS_BUCKET")

  val Json = ("Content-Type" -> "application/json; charset=utf-8")

  def getMaterials(itemId: String) = ApiAction {
    request =>

      Item.findOneById(new ObjectId(itemId)) match {
        case Some(item) => {
          Ok(toJson(item.supportingMaterials))
        }
        case _ => NotFound
      }
  }

  def createMaterial(itemId: String, fileName: String) = Action(
    S3Service.s3upload(AMAZON_ASSETS_BUCKET, itemId + "/materials/" + fileName)
  ) {
    request =>
      Item.findOneById(new ObjectId(itemId)) match {
        case Some(item) => {
          val sm: SupportingMaterialFile = SupportingMaterialFile(fileName, Some(ItemFile(fileName)))
          SupportingMaterialFile.save(sm)
          item.supportingMaterials = item.supportingMaterials :+ sm.id.toString
          Item.save(item)
          Ok(toJson(sm))
        }
        case _ => InternalServerError("Can't find item with id")
      }
  }

  def getMaterial(itemId: String, fileName: String) = ApiAction {
    request =>
      S3Service.s3download(AMAZON_ASSETS_BUCKET, itemId, "/materials/" + fileName)
  }

}
