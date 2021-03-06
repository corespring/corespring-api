package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.futureValidation.FutureValidation
import org.corespring.itemSearch._
import org.corespring.futureValidation.FutureValidation._
import org.corespring.models.item.{ComponentType, Item, PlayerDefinition}
import org.corespring.models.json.{JsonFormatting, JsonUtil}
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.item.ItemService
import org.corespring.services.{CloneItemService, OrgCollectionService, OrganizationService}
import org.corespring.v2.actions.V2Actions
import org.corespring.v2.api.services.ScoreService
import org.corespring.v2.auth.ItemAuth
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.Errors._
import org.corespring.v2.errors.V2Error
import org.corespring.v2.sessiondb.SessionService
import play.api.Logger
import play.api.libs.json._
import play.api.libs.json.Json._
import play.api.mvc._

import scala.concurrent._
import scalaz.Scalaz._
import scalaz.{Failure, Success, Validation}

case class ItemApiExecutionContext(context: ExecutionContext)

class ItemApi(
  actions: V2Actions,
  itemService: ItemService,
  orgService: OrganizationService,
  orgCollectionService: OrgCollectionService,
  cloneItemService: CloneItemService,
  itemIndexService: ItemIndexService,
  itemAuth: ItemAuth[OrgAndOpts],
  itemTypes: Seq[ComponentType],
  scoreService: ScoreService,
  val jsonFormatting: JsonFormatting,
  apiContext: ItemApiExecutionContext,
  sessionService: SessionService) extends V2Api with JsonUtil {

  implicit val itemFormat = jsonFormatting.item
  implicit val pdFormat = jsonFormatting.formatPlayerDefinition

  override implicit def ec: ExecutionContext = apiContext.context

  private implicit val QueryReads = ItemIndexQuery.ApiReads
  private implicit val ItemIndexSearchResultFormat = ItemIndexSearchResult.Format

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
  def create = actions.Org.async { request =>
    import scalaz.Scalaz._
    Future {

      logger.trace(s"function=create jsonBody=${request.body.asJson}")

      val out = for {
        dc <- orgCollectionService.getDefaultCollection(request.org.id).leftMap(e => generalError(e.message))
        json <- loadJson(dc.id)(request)
        validJson <- validatedJson(dc.id)(json).toSuccess(incorrectJsonFormat(json))
        collectionId <- (validJson \ "collectionId").asOpt[String].toSuccess(invalidJson("no collection id specified"))
        canCreate <- itemAuth.canCreateInCollection(collectionId)(request.orgAndOpts)
        item <- validJson.asOpt[Item].toSuccess(invalidJson("can't parse json as Item"))
        vid <- if (canCreate) {
          logger.trace(s"function=create, inserting item, json=${validJson}")
          itemAuth.insert(item)(request.orgAndOpts).toSuccess(errorSaving)
        } else Failure(errorSaving("creation denied"))
      } yield {
        logger.trace(s"new item id: $vid")
        item.copy(id = vid)
      }

      out.map(i => Json.toJson(i)).toSimpleResult()
    }
  }

  private[ItemApi] implicit class JsResultToValidation[T](jsResult: JsResult[T]) {
    def toValidation: Validation[V2Error, T] = jsResult match {
      case JsSuccess(d, _) => Success(d)
      case JsError(errors) => Failure(invalidJson(errors.mkString))
    }
  }

  def search(query: Option[String]) = actions.Org.async { request =>
    logger.debug(s"function=search, query=$query")
    searchWithQueryAndCollections(query, Some(request.org.name), request.org.accessibleCollections.map(_.collectionId): _*) { r => toJson(r) }
  }

  def searchByCollectionId(
    collectionId: ObjectId,
    q: Option[String] = None) = actions.OrgWithStatusCode(BAD_REQUEST).async { request =>
    searchWithQueryAndCollections(q, Some(request.org.name), collectionId) { searchResult =>
      implicit val f = ItemIndexHit.Format
      toJson(searchResult.hits)
    }
  }

  private def searchWithQueryAndCollections(query: Option[String], preference: Option[String], collectionIds: ObjectId*)(mkJson: ItemIndexSearchResult => JsValue): Future[SimpleResult] = {
    (for {
      sq <- fv(QueryStringParser.scopedSearchQuery(query, collectionIds))
      _ <- fv(Success(logger.debug(s"function=searchWithQueryAndCollection, sq=$sq")))
      results <- fv(itemIndexService.search(sq, preference))
    } yield results).future.map { v =>
      v.fold(e => BadRequest(e.getMessage), results => Ok(mkJson(results)))
    }
  }

  def getItemTypes() = Action.async {
    _ =>
      Future {
        val keyValues = itemTypes.map(it => Json.obj("key" -> it.componentType, "value" -> it.label))
        val json = JsArray(keyValues)
        Ok(Json.toJson(json))
      }
  }

  def delete(itemId: String) = actions.Org.async { implicit request =>
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
        vid <- VersionedId(itemId).toSuccess(cantParseItemId(itemId))
        collectionId <- itemService.collectionIdForItem(vid).toSuccess(cantFindItemWithId(vid))
        canDelete <- itemAuth.canCreateInCollection(collectionId.toString)(request.orgAndOpts)
        result <- moveItemToArchive(vid)
      } yield {
        result
      }

      out.map(_ => Json.obj()).toSimpleResult()
    }
  }

  def noPlayerDefinition(id: VersionedId[ObjectId]): V2Error = generalError(s"This item ($id) has no player definition, unable to calculate a score")

  /**
   * Check a score against a given item
   *
   * @param itemId
   * @return
   */
  def checkScore(itemId: String): Action[AnyContent] = actions.Org.async { implicit request =>

    logger.trace(s"function=checkScore itemId=$itemId jsonBody=${request.body.asJson}")

    Future {
      val out: Validation[V2Error, JsValue] = for {
        answers <- request.body.asJson.toSuccess(noJson)
        item <- itemAuth.loadForRead(itemId)(request.orgAndOpts)
        playerDef <- item.playerDefinition.toSuccess(noPlayerDefinition(item.id))
        score <- scoreService.score(playerDef, answers)
      } yield score

      out.toSimpleResult()
    }
  }

  def getFull(itemId: String) = get(itemId, Some("full"))

  def get(itemId: String, detail: Option[String] = None) = actions.Org.async { implicit request =>
    import scalaz.Scalaz._

    Future {
      val out = for {
        vid <- VersionedId(itemId).toSuccess(cantParseItemId(itemId))
        item <- itemAuth.loadForRead(itemId)(request.orgAndOpts)
      } yield jsonFormatting.itemSummary.write(item, detail)

      out.toSimpleResult()
    }
  }

  def cloneItem(id: String) = actions.Org.async { request =>

    lazy val targetCollectionId: Validation[V2Error, Option[ObjectId]] = {
      val rawId: Option[String] = request.body.asJson.flatMap { j =>
        (j \ "collectionId").asOpt[String]
      }

      rawId.map { r =>
        if (ObjectId.isValid(r)) {
          Success(Some(new ObjectId(r)))
        } else {
          Failure(generalError(s"Not a valid object id string: $r"))
        }
      }.getOrElse(Success(None))
    }

    val v: FutureValidation[V2Error, VersionedId[ObjectId]] = for {
      vid <- fv(VersionedId(id).toSuccess(cantParseItemId(id)))
      collectionId <- fv(targetCollectionId)
      r <- cloneItemService.cloneItem(vid, request.org.id, collectionId).leftMap(e => generalError(e.message))
    } yield r

    v.map { vid =>
      Json.obj("id" -> vid.toString)
    }.future.toSimpleResult()

  }

  def publish(id: String) = actions.Org.async { request =>
    Future {
      val out = for {
        vid <- VersionedId(id).toSuccess(cantParseItemId(id))
        published <- Success(itemService.publish(vid))
      } yield Json.obj("id" -> id, "published" -> published)

      out.toSimpleResult()
    }
  }

  def saveNewVersion(id: String) = actions.Org.async { request =>
    Future {
      val out = for {
        vid <- VersionedId(id).toSuccess(cantParseItemId(id))
        newId <- itemService.saveNewUnpublishedVersion(vid).toSuccess(generalError(s"Error saving new version of $id"))
      } yield Json.obj("id" -> newId.toString)

      out.toSimpleResult()
    }
  }

  def countSessions(itemId: VersionedId[ObjectId]) = actions.Org.async { _ =>
    Future {
      val count = sessionService.sessionCount(itemId)
      Ok(Json.obj("sessionCount" -> count))
    }
  }

  private def defaultItem(collectionId: ObjectId): JsValue = validatedJson(collectionId)(Json.obj()).get

  lazy val defaultPlayerDefinition = Json.toJson(PlayerDefinition("<div></div>"))

  private def addIfNeeded[T](json: JsObject, prop: String, defaultValue: JsValue)(implicit r: Format[T]): JsObject = {
    (json \ prop).asOpt[T]
      .map(_ => json)
      .getOrElse {
        logger.trace(s"adding default value - adding $prop as $defaultValue")
        json + (prop -> defaultValue)
      }
  }

  private def addDefaultPlayerDefinition(json: JsObject): JsObject = addIfNeeded[JsObject](json, "playerDefinition", defaultPlayerDefinition)

  private def addDefaultCollectionId(json: JsObject, defaultCollectionId: ObjectId): JsObject = addIfNeeded[String](json, "collectionId", JsString(defaultCollectionId.toString))

  private def validatedJson(defaultCollectionId: ObjectId)(raw: JsValue): Option[JsValue] = raw.asOpt[JsObject].map { rawObj =>
    val noId = (rawObj - "id").as[JsObject]
    val steps = addDefaultPlayerDefinition _ andThen (addDefaultCollectionId(_, defaultCollectionId))
    steps(noId)
  }

  private def loadJson(defaultCollectionId: ObjectId)(request: Request[AnyContent]): Validation[V2Error, JsValue] = {

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
