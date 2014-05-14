package org.corespring.api.v2

import org.bson.types.ObjectId
import org.corespring.api.v2.actions.OrgRequest
import org.corespring.api.v2.actions.V2ItemActions
import org.corespring.api.v2.errors.Errors._
import org.corespring.api.v2.errors.V2ApiError
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.ItemService
import org.slf4j.LoggerFactory
import play.api.libs.json._
import play.api.mvc.{AnyContent, Controller}
import scala.Some
import scala.concurrent.{ExecutionContext, Future}
import scalaz.Failure
import scalaz.Success
import scalaz.Validation
import play.api.libs.json.JsArray
import scalaz.Failure
import play.api.libs.json.JsString
import org.corespring.api.v2.actions.OrgRequest
import org.corespring.api.v2.errors.Errors.generalError
import org.corespring.api.v2.errors.Errors.incorrectJsonFormat
import scalaz.Success
import play.api.libs.json.JsObject

trait ItemApi extends Controller {

  implicit def executionContext : ExecutionContext

  def itemActions: V2ItemActions[AnyContent]

  def itemService: ItemService

  protected lazy val logger = LoggerFactory.getLogger("v2Api.ItemApi")

  private def defaultItem(collectionId: ObjectId): JsValue = {
    validatedJson(collectionId.toString)(Json.obj()).get
  }

  lazy val defaultPlayerDefinition = Json.obj(
    "components" -> Json.obj(),
    "files" -> JsArray(Seq.empty),
    "xhtml" -> "<div></div>"
  )

  private def addIfNeeded[T](json:JsObject, prop:String, defaultValue:JsValue)(implicit r : Format[T]) : JsObject = {
    (json  \ prop).asOpt[T]
      .map(_ => json)
      .getOrElse{
      logger.trace(s"adding default value - adding $prop as $defaultValue")
      json + (prop -> defaultValue)
    }
  }

  private def addDefaultPlayerDefinition(json:JsObject) : JsObject  = addIfNeeded[JsObject](json, "playerDefinition", defaultPlayerDefinition)

  private def addDefaultCollectionId(json:JsObject, defaultCollectionId:String) : JsObject = addIfNeeded[String](json, "collectionId", JsString(defaultCollectionId))

  private def validatedJson(defaultCollectionId:String)(raw: JsValue): Option[JsValue] = raw.asOpt[JsObject].map { rawObj =>
    val noId = (rawObj - "id").as[JsObject]
    val steps = addDefaultPlayerDefinition _ andThen(addDefaultCollectionId (_, defaultCollectionId) )
    steps(noId)
  }


  private def loadJson(defaultCollectionId:ObjectId)(body:AnyContent) : Validation[V2ApiError,JsValue] = {
    body.asJson.map(Success(_))
      .getOrElse{
      if(body.asText.isDefined){
        Failure(invalidJson(body.asText.get))
      } else {
        Success(defaultItem(defaultCollectionId))
      }
    }
  }


  def create = itemActions.create {

    request: OrgRequest[AnyContent] =>

    Future {

      logger.trace("create")
      import scalaz.Scalaz._

      val result: Validation[V2ApiError, Item] = for {
        json <- loadJson(request.defaultCollection)(request.body)
        cleaned <- validatedJson(request.defaultCollection.toString)(json).toSuccess(incorrectJsonFormat(json))
        item <- cleaned.asOpt[Item].toSuccess(generalError(BAD_REQUEST, "Can't parse json as an Item"))
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
        case Failure(e) => Status(e.code)(e.message)
      }
    }
  }
}
