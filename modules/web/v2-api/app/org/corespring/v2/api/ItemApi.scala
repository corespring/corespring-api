package org.corespring.v2.api

import com.mongodb.casbah.Imports._
import org.corespring.itemSearch.{ ItemIndexSearchResult, ItemIndexQuery, ItemIndexService }
import org.corespring.models.item.{ Item, ItemType }
import org.corespring.models.json.{ JsonFormatting, JsonUtil }
import org.corespring.models.{ Organization }
import org.corespring.models.item.Item.Keys._
import org.corespring.services.bootstrap.Services
import org.corespring.services.{SubjectService, StandardService, OrganizationService}
import org.corespring.services.item.ItemService
import org.corespring.v2.api.services.ScoreService
import org.bson.types.ObjectId
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.auth.models.OrgAndOpts

import scala.concurrent._

import org.corespring.v2.auth.ItemAuth
import org.corespring.v2.errors.V2Error
import play.api.Logger
import org.corespring.v2.errors.Errors._
import play.api.libs.json._
import play.api.mvc._
import scalaz.{ Failure, Success, Validation }
import scalaz.Scalaz._

case class ItemApiExecutionContext(context:ExecutionContext)

class ItemApi(
  coreServices : Services,
  itemAuth: ItemAuth[OrgAndOpts],
  itemTypes: Seq[ItemType],
  scoreService: ScoreService,
  val jsonFormatting: JsonFormatting,
  apiContext: ItemApiExecutionContext,
  override val getOrgAndOptionsFn : RequestHeader => Validation[V2Error, OrgAndOpts]) extends V2Api with JsonUtil {

  implicit val itemFormat = jsonFormatting.item

  override implicit def ec: ExecutionContext = apiContext.context

  /**
   * For a known organization (derived from the request) return Some(id)
   * If it is an unknown user return None
   * @param identity
   * @return
   */
  def defaultCollection(implicit identity: OrgAndOpts): Option[String] = orgService.defaultCollection(identity.org).map(_.toString)

  protected lazy val logger = Logger(classOf[ItemApi])

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
   * adding `apiClient` and `playerToken` query parameter to the call
   */
  def create = Action.async { implicit request =>
    import scalaz.Scalaz._
    Future {

      logger.trace(s"function=create jsonBody=${request.body.asJson}")

      val out = for {
        identity <- getOrgAndOptions(request)
        dc <- defaultCollection(identity).toSuccess(noDefaultCollection(identity.org.id))
        json <- loadJson(dc)(request)
        validJson <- validatedJson(dc)(json).toSuccess(incorrectJsonFormat(json))
        collectionId <- (validJson \ "collectionId").asOpt[String].toSuccess(invalidJson("no collection id specified"))
        canCreate <- itemAuth.canCreateInCollection(collectionId)(identity)
        item <- validJson.asOpt[Item].toSuccess(invalidJson("can't parse json as Item"))
        vid <- if (canCreate) {
          logger.trace(s"function=create, inserting item, json=${validJson}")
          itemService.insert(item).toSuccess(errorSaving("Insert failed"))
        } else Failure(errorSaving("creation denied"))
      } yield {
        logger.trace(s"new item id: $vid")
        item.copy(id = vid)
      }
      validationToResult[Item](i => Ok(Json.toJson(i)))(out)
    }
  }

  import Organization._

  def search(query: Option[String]) = Action.async { implicit request =>
    implicit val QueryReads = ItemIndexQuery.ApiReads
    implicit val ItemIndexSearchResultFormat = ItemIndexSearchResult.Format
    val queryString = query.getOrElse("{}")

    getOrgAndOptions(request) match {
      case Success(orgAndOpts) => safeParse(queryString) match {
        case Success(json) => Json.fromJson[ItemIndexQuery](json) match {
          case JsSuccess(query, _) => {
            val accessibleCollections = orgAndOpts.org.accessibleCollections.map(_.collectionId.toString)
            val collections = query.collections.filter(id => accessibleCollections.contains(id))
            val scopedQuery = (collections.isEmpty match {
              case true => query.copy(collections = accessibleCollections)
              case _ => query.copy(collections = collections)
            })
            itemIndexService.search(scopedQuery).map(result => result match {
              case Success(searchResult) => Ok(Json.prettyPrint(Json.toJson(searchResult)))
              case Failure(error) => BadRequest(error.getMessage)
            })
          }
          case _ => future {
            val error = invalidJson(queryString)
            Status(error.statusCode)(error.message)
          }
        }
        case _ => future {
          val error = invalidJson(queryString)
          Status(error.statusCode)(error.message)
        }
      }
      case _ => future {
        val error = invalidToken(request)
        Status(error.statusCode)(error.message)
      }
    }
  }

  def getItemTypes() = Action {
    val keyValues = itemTypes.map { it => Json.obj("key" -> it.key, "value" -> it.value) }
    val json = JsArray(keyValues)
    Ok(Json.prettyPrint(json))
  }

  def delete(itemId: String) = Action.async { implicit request =>
    import scalaz.Scalaz._

    def moveItemToArchive(id: VersionedId[ObjectId]): Validation[V2Error, Boolean] = {
      try {
        itemService.moveItemToArchive(id)
        Success(true)
      } catch {
        case e: RuntimeException => {
          logger.error("Unexpected exception in moveItemToArchive", e)
          Failure(generalError(s"Error deleting item $id"))
        }
      }
    }

    Future {
      val out = for {
        identity <- getOrgAndOptions(request)
        vid <- VersionedId(itemId).toSuccess(cantParseItemId(itemId))
        dbObject <- itemService.findFieldsById(vid, MongoDBObject(collectionId -> 1)).toSuccess(cantFindItemWithId(vid))
        canDelete <- itemAuth.canCreateInCollection(dbObject.get(collectionId).toString)(identity)
        result <- moveItemToArchive(vid)
      } yield {
        result
      }
      validationToResult[Boolean](i => Ok(""))(out)
    }
  }

  def noPlayerDefinition(id: VersionedId[ObjectId]): V2Error = generalError(s"This item ($id) has no player definition, unable to calculate a score")

  /**
   * Check a score against a given item
   * @param itemId
   * @return
   */
  def checkScore(itemId: String): Action[AnyContent] = Action.async { implicit request =>

    logger.trace(s"function=checkScore itemId=$itemId jsonBody=${request.body.asJson}")

    Future {
      val out: Validation[V2Error, JsValue] = for {
        identity <- getOrgAndOptions(request)
        answers <- request.body.asJson.toSuccess(noJson)
        item <- itemAuth.loadForRead(itemId)(identity)
        playerDef <- item.playerDefinition.toSuccess(noPlayerDefinition(item.id))
        score <- scoreService.score(playerDef, answers)
      } yield score

      validationToResult[JsValue](j => Ok(j))(out)
    }
  }


  def get(itemId: String, detail: Option[String] = None) = Action.async { implicit request =>
    import scalaz.Scalaz._

    Future {
      val out = for {
        vid <- VersionedId(itemId).toSuccess(cantParseItemId(itemId))
        identity <- getOrgAndOptions(request)
        item <- itemAuth.loadForRead(itemId)(identity)
      } yield jsonFormatting.itemSummary.write(item, detail)

      validationToResult[JsValue](i => Ok(i))(out)
    }
  }

  def cloneItem(id: String) = Action.async { implicit request =>
    Future {
      val out = for {
        identity <- getOrgAndOptions(request)
        vid <- VersionedId(id).toSuccess(cantParseItemId(id))
        item <- itemAuth.loadForRead(id)(identity)
        cloned <- itemService.clone(item).toSuccess(generalError(s"Error cloning item with id: $id"))
      } yield Json.obj("id" -> cloned.id.toString)
      validationToResult[JsValue](i => Ok(i))(out)
    }
  }

  def publish(id: String) = Action.async { implicit request =>
    Future {
      val out = for {
        _ <- getOrgAndOptions(request)
        vid <- VersionedId(id).toSuccess(cantParseItemId(id))
        published <- Success(itemService.publish(vid))
      } yield Json.obj("id" -> id, "published" -> published)

      validationToResult[JsValue](i => Ok(i))(out)
    }
  }

  def saveNewVersion(id: String) = Action.async { implicit request =>
    Future {
      val out = for {
        _ <- getOrgAndOptions(request)
        vid <- VersionedId(id).toSuccess(cantParseItemId(id))
        newId <- itemService.saveNewUnpublishedVersion(vid).toSuccess(generalError(s"Error saving new version of $id"))
      } yield Json.obj("id" -> newId.toString)

      validationToResult[JsValue](i => Ok(i))(out)
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
