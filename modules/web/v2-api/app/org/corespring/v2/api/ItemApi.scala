package org.corespring.v2.api

import org.corespring.v2.auth.models.OrgAndOpts

import scala.concurrent.Future

import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.services.item.ItemService
import org.corespring.v2.auth.ItemAuth
import org.corespring.v2.errors.V2Error
import org.corespring.v2.log.V2LoggerFactory
import org.corespring.v2.errors.Errors._
import play.api.libs.json._
import play.api.mvc.{ Action, AnyContent, Request, RequestHeader }
import scalaz.{ Failure, Success, Validation }

trait ItemApi extends V2Api {

  def itemAuth: ItemAuth[OrgAndOpts]
  def itemService: ItemService

  /**
   * For a known organization (derived from the request) return Some(id)
   * If it is an unknown user return None
   * @param identity
   * @return
   */
  def defaultCollection(implicit identity: OrgAndOpts): Option[String]

  protected lazy val logger = V2LoggerFactory.getLogger("ItemApi")

  /**
   * Create an Item. Will set the collectionId to the default id for the
   * requestor's Organization.
   *
   * ## Authentication
   *
   * Requires that the request is authenticated. This can be done using the following means:
   *
   * UserSession authentication (only possible when using the tagger app)
   * adding an `access_token` query parameter to the call
   * adding `apiClient` and `options` query parameter to the call
   */
  def create = Action.async { implicit request =>
    import scalaz.Scalaz._
    Future {
      val out = for {
        identity <- getOrgIdAndOptions(request)
        dc <- defaultCollection(identity).toSuccess(noDefaultCollection(identity.orgId))
        json <- loadJson(dc)(request)
        validJson <- validatedJson(dc)(json).toSuccess(incorrectJsonFormat(json))
        collectionId <- (validJson \ "collectionId").asOpt[String].toSuccess(invalidJson("no collection id specified"))
        canCreate <- itemAuth.canCreateInCollection(collectionId)(identity)
        item <- validJson.asOpt[Item].toSuccess(invalidJson("can't parse json as Item"))
        vid <- if (canCreate)
          itemService.insert(item).toSuccess(errorSaving("Insert failed"))
        else
          Failure(errorSaving("creation denied"))
      } yield {
        logger.trace(s"new item id: $vid")
        item.copy(id = vid)
      }
      validationToResult[Item](i => Ok(Json.toJson(i)))(out)
    }
  }

  private def defaultItem(collectionId: String): JsValue = validatedJson(collectionId)(Json.obj()).get

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

  private def loadJson(defaultCollectionId: String)(request: Request[AnyContent]): Validation[V2Error, JsValue] = {

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
