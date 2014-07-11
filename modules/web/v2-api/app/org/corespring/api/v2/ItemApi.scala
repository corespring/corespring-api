package org.corespring.api.v2

import scala.concurrent.Future

import org.corespring.api.v2.errors.V2ApiError
import org.corespring.api.v2.errors.Errors._
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.ItemService
import org.corespring.v2.auth.ItemAuth
import org.slf4j.LoggerFactory
import play.api.libs.json._
import play.api.mvc._
import scalaz.{ Failure, Success, Validation }

trait ItemApi extends V2Api {

  def itemAuth: ItemAuth
  def itemService: ItemService

  /**
   * For a known organization (derived from the request) return Some(id)
   * If it is an unknown user return None
   * @param header
   * @return
   */
  def defaultCollection(implicit header: RequestHeader): Option[String]

  protected lazy val logger = LoggerFactory.getLogger("v2Api.ItemApi")

  /**
   * POST no content type + empty body ==> need header
   * POST content type json + empty body ==> bad request / invalid json
   * POST content type json + {} ==> new item
   */
  def create = Action.async { implicit request =>
    import scalaz.Scalaz._
    Future {
      val out = for {
        json <- loadJson(defaultCollection)(request)
        validJson <- validatedJson(defaultCollection)(json).toSuccess(incorrectJsonFormat(json))
        collectionId <- (json \ "collectionId").asOpt[String].toSuccess(invalidJson("no collection id specified"))
        canCreate <- itemAuth.canCreateInCollection(collectionId).leftMap(e => generalError(UNAUTHORIZED, e))
        item <- validJson.asOpt[Item].toSuccess(invalidJson("can't parse json as Item"))
        vid <- if (canCreate)
          itemService.insert(item).toSuccess(errorSaving)
        else
          Failure(errorSaving)
      } yield {
        logger.trace(s"new item id: $vid")
        item.copy(id = vid)
      }
      validationToResult[Item](i => Ok(Json.toJson(i)))(out)
    }
  }

  private def defaultItem(collectionId: Option[String]): JsValue = validatedJson(collectionId)(Json.obj()).get

  lazy val defaultPlayerDefinition = Json.obj(
    "components" -> Json.obj(),
    "files" -> JsArray(Seq.empty),
    "xhtml" -> "<div></div>",
    "summaryFeedback" -> "")

  private def addIfNeeded[T](json: JsObject, prop: String, defaultValue: JsValue)(implicit r: Format[T]): JsObject = {
    (json \ prop).asOpt[T]
      .map(_ => json)
      .getOrElse {
        logger.trace(s"adding default value - adding $prop as $defaultValue")
        json + (prop -> defaultValue)
      }
  }

  private def addDefaultPlayerDefinition(json: JsObject): JsObject = addIfNeeded[JsObject](json, "playerDefinition", defaultPlayerDefinition)

  private def addDefaultCollectionId(json: JsObject, defaultCollectionId: Option[String]): JsObject = {
    defaultCollectionId.map { dci =>
      addIfNeeded[String](json, "collectionId", JsString(dci))
    }.getOrElse(json)
  }

  private def validatedJson(defaultCollectionId: Option[String])(raw: JsValue): Option[JsValue] = raw.asOpt[JsObject].map { rawObj =>
    val noId = (rawObj - "id").as[JsObject]
    val steps = addDefaultPlayerDefinition _ andThen (addDefaultCollectionId(_, defaultCollectionId))
    steps(noId)
  }

  private def loadJson(defaultCollectionId: Option[String])(request: Request[AnyContent]): Validation[V2ApiError, JsValue] = {

    def hasJsonHeader: Boolean = {
      val types = Seq("application/json", "text/json")
      request.headers.get(CONTENT_TYPE).map { h =>
        types.contains(h)
      }.getOrElse(false)
    }

    request.body.asJson.map(Success(_))
      .getOrElse {
        if (hasJsonHeader) {
          Success(defaultItem(defaultCollectionId))
        } else {
          Failure(needJsonHeader)
        }
      }
  }
}
