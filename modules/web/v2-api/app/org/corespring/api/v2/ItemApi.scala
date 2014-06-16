package org.corespring.api.v2

import org.bson.types.ObjectId
import org.corespring.api.v2.actions.OrgRequest
import org.corespring.api.v2.actions.V2ApiActions
import org.corespring.api.v2.errors.Errors._
import org.corespring.api.v2.errors.Errors.generalError
import org.corespring.api.v2.errors.Errors.incorrectJsonFormat
import org.corespring.api.v2.errors.Errors.unAuthorized
import org.corespring.api.v2.errors.V2ApiError
import org.corespring.api.v2.services._
import org.corespring.platform.core.models.Organization
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.ItemService
import org.corespring.v2.auth.services.OrgService
import org.slf4j.LoggerFactory
import play.api.libs.json._
import play.api.mvc._
import scala.concurrent.{ ExecutionContext, Future }
import scalaz.Failure
import scalaz.Success
import scalaz.Validation

trait ItemApi extends Controller {

  implicit def executionContext: ExecutionContext

  def actions: V2ApiActions[AnyContent]

  def itemService: ItemService

  def permissionService: PermissionService[Organization, Item]

  def orgService: OrgService

  protected lazy val logger = LoggerFactory.getLogger("v2Api.ItemApi")

  private def defaultItem(collectionId: ObjectId): JsValue = {
    validatedJson(collectionId.toString)(Json.obj()).get
  }

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

  private def addDefaultCollectionId(json: JsObject, defaultCollectionId: String): JsObject = addIfNeeded[String](json, "collectionId", JsString(defaultCollectionId))

  private def validatedJson(defaultCollectionId: String)(raw: JsValue): Option[JsValue] = raw.asOpt[JsObject].map { rawObj =>
    val noId = (rawObj - "id").as[JsObject]
    val steps = addDefaultPlayerDefinition _ andThen (addDefaultCollectionId(_, defaultCollectionId))
    steps(noId)
  }

  private def loadJson(defaultCollectionId: ObjectId)(request: Request[AnyContent]): Validation[V2ApiError, JsValue] = {

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

  private def toValidation(r: PermissionResult): Validation[V2ApiError, Boolean] = {
    r match {
      case Granted => Success(true)
      case Denied(reasons) => Failure(unAuthorized(reasons))
    }
  }

  /**
   * POST no content type + empty body ==> need header
   * POST content type json + empty body ==> bad request / invalid json
   * POST content type json + {} ==> new item
   * @return
   */
  def create = actions.orgAction(BodyParsers.parse.anyContent) { request: OrgRequest[AnyContent] =>
    Future {
      logger.trace("create")
      import scalaz.Scalaz._
      val result: Validation[V2ApiError, Item] = for {
        json <- loadJson(request.defaultCollection)(request)
        cleaned <- validatedJson(request.defaultCollection.toString)(json).toSuccess(incorrectJsonFormat(json))
        item <- cleaned.asOpt[Item].toSuccess(generalError(BAD_REQUEST, "Can't parse json as an Item"))
        org <- orgService.org(request.orgId).toSuccess(generalError(BAD_REQUEST, s"Can't find org: ${request.orgId}"))
        permission <- toValidation(permissionService.create(org, item))
        vid <- itemService.insert(item).toSuccess(errorSaving)
      } yield {
        logger.trace(s"new item id: $vid")
        item.copy(id = vid)
      }

      result match {
        case Success(item) => {
          logger.trace(s"return item: $item")
          Ok(Json.toJson(item))
        }
        case Failure(e) => Status(e.code)(Json.obj("error" -> e.message))
      }
    }
  }
}
