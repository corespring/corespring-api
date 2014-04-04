package org.corespring.player.accessControl.auth

import play.api.mvc.{AnyContent, Request, Result}
import org.corespring.player.accessControl.models.RenderOptions
import org.corespring.platform.core.models.auth.ApiClient
import scalaz.{Failure, Success, Validation}
import org.bson.types.ObjectId
import play.api.libs.json.{Json, JsValue}
import org.corespring.common.encryption.{Crypto, AESCrypto}
import scalaz.Scalaz._


trait AuthParams {

  private var crypto: Crypto = AESCrypto

  def changeCrypto(newCrypto: Crypto) = crypto = newCrypto

  def withOptions(errorBlock: String => Result)(block: Option[RenderOptions] => Result)(implicit request: Request[AnyContent], client: ApiClient) = {

    import AuthParamErrorMessages._

    val result: Validation[String, Result] = for {
      o <- request.queryString.get("options").map(_.mkString).toSuccess(queryParamNotFound("options", request.queryString))
      ro <- decryptOptions(o, client)
    } yield block(Some(ro))

    result match {
      case Success(r) => r
      case Failure(msg) => errorBlock(msg)
    }
  }

  def withApiClient(errorBlock: String => Result)(block: ApiClient => Result)(implicit request: Request[AnyContent]): Result = {

    import AuthParamErrorMessages._

    val result: Validation[String, Result] = for {
      id <- request.queryString.get("apiClientId").map(_.mkString).toSuccess(queryParamNotFound("apiClientId", request.queryString))
      validId <- if (ObjectId.isValid(id)) Success(id) else Failure(InvalidObjectId)
      client <- ApiClient.findByKey(id).toSuccess(apiClientNotFound(id))
    } yield block(client)

    result match {
      case Success(r) => r
      case Failure(msg) => errorBlock(msg)
    }
  }

  def decryptOptions(encryptedOptions: String, apiClient: ApiClient): Validation[String, RenderOptions] = {

    import AuthParamErrorMessages._

    def decryptString = try {
      Success(crypto.decrypt(encryptedOptions, apiClient.clientSecret))
    } catch {
      case e: Throwable => Failure(e.getMessage)
    }

    def parse(s: String): Validation[String, JsValue] = try {
      Success(Json.parse(s))
    } catch {
      case e: Throwable => Failure(badJsonString(s, e))
    }

    for {
      s <- decryptString
      parsed <- parse(s)
      ro <- parsed.asOpt[RenderOptions].toSuccess(cantConvertJsonToRenderOptions(s))
    } yield ro
  }

}

object AuthParamErrorMessages {

  def apiClientNotFound(id: String) = "Can't find api client with id: " + id
  def queryParamNotFound(key: String, queryString: Map[String, Seq[String]]) = "Can't find parameter '" + key + "' on query string: " + queryString
  val InvalidObjectId = "Invalid ObjectId"
  def badJsonString(s: String, e: Throwable) = escape("Can't parse string into json: " + s)
  def cantConvertJsonToRenderOptions(s: String) = escape("Can't convert json to options: " + s)

  private def escape(s: String): String = {
    val escaped = s.replace("\"", "\\\\\"")
    escaped
  }
}
