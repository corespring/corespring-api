package controllers.auth

import play.api.libs.json._
import org.bson.types.ObjectId
import play.api.libs.json.JsArray
import play.api.libs.json.JsObject
import scala.Some
import models.itemSession.ItemSessionSettings
import models.auth.ApiClient
import encryption.AESCrypto

case class RenderOptions(itemId:Option[String] = None, sessionId:Option[String] = None, assessmentId:Option[String] = None, role:Option[String] = None, expires:Long, mode:String)
object RenderOptions{
  val ALL_ITEMS = "*"
  /**
   *
   * @param constraints1 the constraints in question
   * @param constraints2 the constraints
   * @return
   */
  def authCheck(constraints1:RenderOptions, constraints2:RenderOptions):Boolean = {
    constraints1.itemId.map(itemId1 => constraints2.itemId.exists(itemId2 => itemId2 == itemId1 || itemId2 == ALL_ITEMS)).getOrElse(true) &&
    constraints1.sessionId.map(itemSessionId1 => constraints2.sessionId.exists(itemSessionId2 => itemSessionId2 == itemSessionId1 || itemSessionId2 == ALL_ITEMS)).getOrElse(true) &&
    constraints1.assessmentId.map(assessmentId1 => constraints2.assessmentId.exists(assessmentId2 => assessmentId2 == assessmentId1)).getOrElse(true) &&
    constraints1.role == constraints2.role && constraints1.mode == constraints2.mode &&
    (constraints1.expires < System.currentTimeMillis() || constraints1.expires == 0)
  }
  implicit object RCReads extends Reads[RenderOptions]{
    def reads(json:JsValue):RenderOptions = {
      RenderOptions(
        (json \ "itemId").asOpt[String],
        (json \ "sessionId").asOpt[String],
        (json \ "assessmentId").asOpt[String],
        (json \ "role").asOpt[String],
        (json \ "expires").as[Long],
        (json \ "mode").as[String]
      )
    }
  }
  implicit object RCWrites extends Writes[RenderOptions]{
    def writes(rc:RenderOptions):JsValue = {
      JsObject(Seq(
        "itemId" -> rc.itemId.map(JsString(_)),
        "sessionId" -> rc.sessionId.map(JsString(_)),
        "assessmentId" -> rc.assessmentId.map(JsString(_)),
        "role" -> rc.role.map(JsString(_)),
        "expires" -> Some(JsNumber(rc.expires)),
        "mode" -> Some(JsString(rc.mode))
      ).filter(_._2.isDefined).map(prop => (prop._1,prop._2.get)))
    }
  }
//  def decryptOptions(clientId:String, encrypted:String):Option[RenderOptions] = {
//    ApiClient.findByKey(clientId) match {
//      case Some(apiClient) => Some(decryptOptions(apiClient,encrypted))
//      case None => None
//    }
//  }
  def decryptOptions(apiClient:ApiClient, encrypted:String):RenderOptions = {
    val decrypted = AESCrypto.decrypt(encrypted,apiClient.clientSecret)
    Json.fromJson[RenderOptions](Json.parse(decrypted))
  }
}
case class RendererContext(apiClient: ApiClient, options:RenderOptions)
object RendererContext{
  def decryptContext(clientId:String,encrypted:String):Option[RendererContext] = {
    ApiClient.findByKey(clientId) match {
      case Some(apiClient) =>
        Some(RendererContext(apiClient, RenderOptions.decryptOptions(apiClient,encrypted)))
      case None => None
    }
  }
  implicit object ContextReads extends Reads[RendererContext]{
    def reads(json:JsValue):RendererContext = {
      val clientId = (json \ "clientId").as[String]
      ApiClient.findByKey(clientId) match {
        case Some(apiClient) => {
          (json \ "options") match {
            case optobj:JsObject => RendererContext(apiClient, Json.fromJson[RenderOptions](optobj))
            case JsString(encrypted) => RendererContext(apiClient, RenderOptions.decryptOptions(apiClient,encrypted))
            case _ => throw new RuntimeException("could not parse render options")
          }
        }
        case None => throw new RuntimeException("no api client found")
      }
    }
  }
  implicit object ContextWrites extends Writes[RendererContext]{
    def writes(ctx:RendererContext):JsValue = {
      JsObject(Seq(
        "clientId" -> JsString(ctx.apiClient.clientId.toString),
        "options" -> Json.toJson(ctx.options)
      ))
    }
  }
}
