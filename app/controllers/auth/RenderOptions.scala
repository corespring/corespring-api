package controllers.auth

import encryption.AESCrypto
import models.auth.ApiClient
import play.api.libs.json._


case class RenderOptions(
                          itemId: String = "*",
                          sessionId: String = "*",
                          assessmentId: String = "*",
                          collectionId : String = "*",
                          role: String = "student",
                          expires: Long, mode: String) {

  def allowSessionId(id:String) : Boolean = allow(id, sessionId)
  def allowItemId(id: String): Boolean = allow(id,itemId)
  def allowAssessmentId(id: String): Boolean = allow(id,assessmentId)
  def allowCollectionId(id:String) : Boolean = allow(id, collectionId)

  def allowMode(m:String) : Boolean = allow(m,mode)

  private def allow(id:String, optionId:String) = if(optionId == RenderOptions.*) true else id == optionId

}

object RenderOptions {

  val * = "*"

  val ANYTHING = RenderOptions(expires = 0, mode = *)

  implicit object RCReads extends Reads[RenderOptions] {
    def reads(json: JsValue): RenderOptions = {
      RenderOptions(
        (json \ "itemId").asOpt[String].getOrElse(*),
        (json \ "sessionId").asOpt[String].getOrElse(*),
        (json \ "assessmentId").asOpt[String].getOrElse(*),
        (json \ "collectionId").asOpt[String].getOrElse(*),
        (json \ "role").asOpt[String].getOrElse("student"),
        (json \ "expires").as[Long],
        (json \ "mode").as[String]
      )
    }
  }

  implicit object RCWrites extends Writes[RenderOptions] {
    def writes(ro: RenderOptions): JsValue = {
      JsObject(Seq(
        "itemId" -> JsString(ro.itemId),
        "sessionId" -> JsString(ro.sessionId),
        "assessmentId" -> JsString(ro.assessmentId),
        "collectionId" -> JsString(ro.collectionId),
        "role" -> JsString(ro.role),
        "expires" -> JsNumber(ro.expires),
        "mode" -> JsString(ro.mode)
      ))
    }
  }

  def decryptOptions(apiClient: ApiClient, encrypted: String): RenderOptions = {
    val decrypted = AESCrypto.decrypt(encrypted, apiClient.clientSecret)
    Json.fromJson[RenderOptions](Json.parse(decrypted))
  }
}
