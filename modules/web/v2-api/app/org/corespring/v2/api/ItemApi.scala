package org.corespring.v2.api

import org.bson.types.ObjectId
import org.corespring.itemSearch._
import org.corespring.futureValidation.FutureValidation._
import org.corespring.models.item.{ ComponentType, Item }
import org.corespring.models.json.{ JsonFormatting, JsonUtil }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.item.ItemService
import org.corespring.services.{ CloneItemService, OrgCollectionService, OrganizationService }
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
import scalaz.{ Failure, Success, Validation }

case class ItemApiExecutionContext(context: ExecutionContext)

class ItemApi(
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
  sessionService: SessionService,
  override val getOrgAndOptionsFn: RequestHeader => Validation[V2Error, OrgAndOpts]) extends V2Api with JsonUtil {

  implicit val itemFormat = jsonFormatting.item

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
  def create = Action.async { implicit request =>
    import scalaz.Scalaz._
    Future {

      logger.trace(s"function=create jsonBody=${request.body.asJson}")

      val out = for {
        identity <- getOrgAndOptions(request)
        dc <- orgCollectionService.getDefaultCollection(identity.org.id).leftMap(e => generalError(e.message))
        json <- loadJson(dc.id)(request)
        validJson <- validatedJson(dc.id)(json).toSuccess(incorrectJsonFormat(json))
        collectionId <- (validJson \ "collectionId").asOpt[String].toSuccess(invalidJson("no collection id specified"))
        canCreate <- itemAuth.canCreateInCollection(collectionId)(identity)
        item <- validJson.asOpt[Item].toSuccess(invalidJson("can't parse json as Item"))
        vid <- if (canCreate) {
          logger.trace(s"function=create, inserting item, json=${validJson}")
          itemAuth.insert(item)(identity).toSuccess(errorSaving)
        } else Failure(errorSaving("creation denied"))
      } yield {
        logger.trace(s"new item id: $vid")
        item.copy(id = vid)
      }
      validationToResult[Item](i => Ok(Json.toJson(i)))(out)
    }
  }

  private[ItemApi] implicit class JsResultToValidation[T](jsResult: JsResult[T]) {
    def toValidation: Validation[V2Error, T] = jsResult match {
      case JsSuccess(d, _) => Success(d)
      case JsError(errors) => Failure(invalidJson(errors.mkString))
    }
  }

  def search(query: Option[String]) = futureWithIdentity { (identity, request) =>
    logger.debug(s"function=search, query=$query")
    searchWithQueryAndCollections(query, identity.org.accessibleCollections.map(_.collectionId): _*) { r => toJson(r) }
  }

  def searchByCollectionId(
    collectionId: ObjectId,
    q: Option[String] = None) = futureWithIdentity(BAD_REQUEST) { (identity, request) =>
    searchWithQueryAndCollections(q, collectionId) { searchResult =>
      implicit val f = ItemIndexHit.Format
      toJson(searchResult.hits)
    }
  }

  private def searchWithQueryAndCollections(query: Option[String], collectionIds: ObjectId*)(mkJson: ItemIndexSearchResult => JsValue): Future[SimpleResult] = {
    (for {
      sq <- fv(QueryStringParser.scopedSearchQuery(query, collectionIds))
      results <- fv(itemIndexService.search(sq))
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
        collectionId <- itemService.collectionIdForItem(vid).toSuccess(cantFindItemWithId(vid))
        canDelete <- itemAuth.canCreateInCollection(collectionId.toString)(identity)
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
   *
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

  def getFull(itemId: String) = get(itemId, Some("full"))

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

  def cloneItem(id: String) = futureWithIdentity((identity, request) => {

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

    VersionedId(id).map { vid =>
      targetCollectionId match {
        case Success(collectionId) => {
          cloneItemService.cloneItem(vid, identity.org.id, collectionId).future.map { v =>
            val asJson = v.bimap(
              e => generalError(s"Error cloning item with id: $id: ${e.message}"),
              id => Json.obj("id" -> id.toString))
            validationToResult[JsValue](i => Ok(i))(asJson)
          }
        }
        case Failure(e) => Future(e.toResult)
      }
    }.getOrElse(Future(cantParseItemId(id).toResult))
  })

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

  def countSessions(itemId: VersionedId[ObjectId]) = Action.async { implicit request =>
    Future {
      val count = sessionService.sessionCount(itemId)
      Ok(Json.obj("sessionCount" -> count))
    }
  }

  private def defaultItem(collectionId: ObjectId): JsValue = validatedJson(collectionId)(Json.obj()).get

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
