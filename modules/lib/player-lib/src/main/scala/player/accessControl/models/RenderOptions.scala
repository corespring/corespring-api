package player.accessControl.models

import play.api.libs.json._
import org.corespring.platform.core.models.auth.ApiClient
import org.corespring.common.encryption.AESCrypto

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

      def expires : Option[Long] = (json \ "expires").asOpt[Long] orElse (json \ "expires").asOpt[String].map(_.toLong)

      expires.map{
        e =>
          JsSuccess(RenderOptions(
            (json \ "itemId").asOpt[String].filterNot(_.isEmpty).getOrElse(*),
            (json \ "sessionId").asOpt[String].filterNot(_.isEmpty).getOrElse(*),
            (json \ "assessmentId").asOpt[String].filterNot(_.isEmpty).getOrElse(*),
            (json \ "role").asOpt[String].filterNot(_.isEmpty).getOrElse("student"),
            e,
            RequestedAccess.Mode.withName((json \ "mode").as[String])
          ))
      }.getOrElse(JsError("Expires is a mandatory field"))
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
