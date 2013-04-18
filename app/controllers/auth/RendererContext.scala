package controllers.auth

import play.api.libs.json._
import org.bson.types.ObjectId
import play.api.libs.json.JsArray
import play.api.libs.json.JsObject
import scala.Some
import models.itemSession.ItemSessionSettings
import models.auth.ApiClient

case class RenderConstraints(itemId:Option[String] = None, itemSessionId:Option[String] = None, assessmentId:Option[String] = None, settings:Option[ItemSessionSettings] = None, expires:Long)
object RenderConstraints{
  val ALL_ITEMS = "*"
  /**
   *
   * @param constraints1 the constraints in question
   * @param constraints2 the constraints
   * @return
   */
  def authCheck(constraints1:RenderConstraints, constraints2:RenderConstraints):Boolean = {
    constraints1.itemId.map(itemId1 => constraints2.itemId.exists(itemId2 => itemId2 == itemId1 || itemId2 == ALL_ITEMS)).getOrElse(true) &&
    constraints1.itemSessionId.map(itemSessionId1 => constraints2.itemSessionId.exists(itemSessionId2 => itemSessionId2 == itemSessionId1 || itemSessionId2 == ALL_ITEMS)).getOrElse(true) &&
    constraints1.assessmentId.map(assessmentId1 => constraints2.assessmentId.exists(assessmentId2 => assessmentId2 == assessmentId1 || assessmentId2 == ALL_ITEMS)).getOrElse(true) &&
    constraints1.settings.map(itemSessionSettings1 => constraints2.itemSessionId.exists(itemSessionSettings2 => itemSessionSettings1 == itemSessionSettings2)).getOrElse(true) &&
    (constraints1.expires < System.currentTimeMillis() || constraints1.expires == 0)
  }
  implicit object RCReads extends Reads[RenderConstraints]{
    def reads(json:JsValue):RenderConstraints = {
      RenderConstraints(
        (json \ "itemId").asOpt[String],
        (json \ "itemSessionId").asOpt[String],
        (json \ "assessmentId").asOpt[String],
        (json \ "settings").asOpt[ItemSessionSettings],
        (json \ "expires").as[Long]
      )
    }
  }
  implicit object RCWrites extends Writes[RenderConstraints]{
    def writes(rc:RenderConstraints):JsValue = {
      JsObject(Seq(
        "itemId" -> rc.itemId.map(JsString(_)),
        "settings" -> rc.settings.map(Json.toJson(_)),
        "itemSessionId" -> rc.itemSessionId.map(JsString(_)),
        "assessmentId" -> rc.assessmentId.map(JsString(_)),
        "expires" -> Some(JsNumber(rc.expires))
      ).filter(_._2.isDefined).map(prop => (prop._1,prop._2.get)))
    }
  }
}
case class RendererContext(clientId: String, rc:RenderConstraints)
object RendererContext{
  val keyDelimeter = "-"
  def parserRendererContext(key:String):Option[RendererContext] = {
    val parts = key.split(keyDelimeter)
    if (parts.length == 2){
      ApiClient.findByKey(parts(0)) match {
        case Some(apiClient) => {
          val decrypted = AESCrypto.decryptAES(parts(1),apiClient.clientSecret)
          val rc = Json.fromJson[RenderConstraints](Json.parse(decrypted))
          Some(RendererContext(parts(0),rc))
        }
        case None => None
      }
    } else None
  }
}
