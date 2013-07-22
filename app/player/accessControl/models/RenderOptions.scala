package player.accessControl.models

import common.encryption.AESCrypto
import models.auth.ApiClient
import play.api.libs.json.JsNumber
import play.api.libs.json.JsObject
import play.api.libs.json.JsString
import play.api.libs.json._

case class RenderOptions(itemId: String = "*",
                         sessionId: String = "*",
                         assessmentId: String = "*",
                         role: String = "student",
                         expires: Long,
                         mode: RequestedAccess.Mode.Mode) {
  def allowItemId(id: String): Boolean = if (itemId == RenderOptions.*) { true  } else id == itemId
}


object RenderOptions {

  val * = "*"

  val ANYTHING = RenderOptions(expires = 0, mode = RequestedAccess.Mode.All)

  implicit object Reads extends Reads[RenderOptions] {
    def reads(json: JsValue): JsResult[RenderOptions] = {

      RequestedAccess.Mode.withName("preview")
      JsSuccess(RenderOptions(
        (json \ "itemId").asOpt[String].filterNot(_.isEmpty).getOrElse(*),
        (json \ "sessionId").asOpt[String].filterNot(_.isEmpty).getOrElse(*),
        (json \ "assessmentId").asOpt[String].filterNot(_.isEmpty).getOrElse(*),
        (json \ "role").asOpt[String].filterNot(_.isEmpty).getOrElse("student"),
        (json \ "expires").as[Long],
        RequestedAccess.Mode.withName((json \ "mode").as[String])
      ))
    }
  }

  implicit object Writes extends Writes[RenderOptions] {
    def writes(ro: RenderOptions): JsValue = {
      JsObject(Seq(
        "itemId" -> JsString(ro.itemId),
        "sessionId" -> JsString(ro.sessionId),
        "assessmentId" -> JsString(ro.assessmentId),
        "role" -> JsString(ro.role),
        "expires" -> JsNumber(ro.expires),
        "mode" -> JsString(ro.mode.toString)
      ))
    }
  }

  def decryptOptions(apiClient: ApiClient, encrypted: String): RenderOptions = {
    val decrypted = AESCrypto.decrypt(encrypted, apiClient.clientSecret)
    Json.fromJson[RenderOptions](Json.parse(decrypted)) match {
      case JsSuccess(ro, _) => ro
      case JsError(e) => throw new RuntimeException("Error parsing json")
    }
  }
}
