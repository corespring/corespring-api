package controllers.auth

import models.auth.ApiClient
import play.api.libs.json._
import scala.Some


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
