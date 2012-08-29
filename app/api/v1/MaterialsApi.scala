package api.v1

import controllers.auth.BaseApi
import com.typesafe.config.ConfigFactory
import play.api.mvc.Action
import controllers.S3Service
import common.models.{SupportingMaterial, SupportingMaterialFile}
import models.{Item, ItemFile}
import org.bson.types.ObjectId
import play.api.libs.json.Json._
import api.ApiError
import play.api.Logger

object MaterialsApi extends BaseApi {

  private final val AMAZON_ASSETS_BUCKET: String = ConfigFactory.load().getString("AMAZON_ASSETS_BUCKET")

  val Json = ("Content-Type" -> "application/json; charset=utf-8")

  def uploadMaterial(itemId: String, fileName: String) = Action(
    S3Service.s3upload(AMAZON_ASSETS_BUCKET, itemId + "/materials/" + fileName)
  ) {
    request =>
      Item.findOneById(new ObjectId(itemId)) match {
        case Some(item) => {
          val f = SupportingMaterialFile(fileName, Some(fileName))
          item.supportingMaterials = item.supportingMaterials :+ f
          Item.save(item)
          Ok(toJson(f.asInstanceOf[SupportingMaterial]))
        }
        case _ => InternalServerError("Can't find item with id")
      }
  }

  def getMaterial(itemId: String, fileName: String) = ApiAction {
    request =>
      S3Service.s3download(AMAZON_ASSETS_BUCKET, itemId, "/materials/" + fileName)
  }

}
