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
import play.api.libs.json._
import common.models.SupportingMaterialFile
import play.api.libs.json.JsObject
import play.api.libs.json.JsBoolean
import play.api.libs.json.JsString
import scala.Some
import models.mongoContext._

object MaterialsApi extends BaseApi {

  private final val AMAZON_ASSETS_BUCKET: String = ConfigFactory.load().getString("AMAZON_ASSETS_BUCKET")

  val Json = ("Content-Type" -> "application/json; charset=utf-8")

  def uploadMaterial(itemId: String, name:String) = Action(
    S3Service.s3upload(AMAZON_ASSETS_BUCKET, itemId + "/materials/" + name)
  ) {
    request =>
      Item.findOneById(new ObjectId(itemId)) match {
        case Some(item) => {
          val f = SupportingMaterialFile(name)
          item.supportingMaterials = item.supportingMaterials :+ f
          Item.save(item)
          Ok(toJson(f.asInstanceOf[SupportingMaterial]))
        }
        case _ => InternalServerError("Can't find item with id")
      }
  }

  def getMaterial(itemId: String, fileName: String) = Action {
    request =>
      S3Service.s3download(AMAZON_ASSETS_BUCKET, itemId, "materials/" + fileName)
  }

  implicit object DeleteResponseWrites extends Writes[S3Service.S3DeleteResponse] {
    def writes(obj:S3Service.S3DeleteResponse) = {
      var seq : Seq[(String,JsValue)] = Seq()
      seq = seq :+ ("success" -> JsBoolean(obj.success))
      seq = seq :+ ("msg" -> JsString(obj.msg))
      JsObject(seq)
    }
  }

  def deleteMaterial(itemId:String, fileName:String) = Action{
   request =>
      val response : S3Service.S3DeleteResponse = S3Service.delete( AMAZON_ASSETS_BUCKET, itemId + "/materials/" + fileName )
      Ok(toJson(response))
  }


}


