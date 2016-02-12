package org.corespring.v2.player.hooks

import org.bson.types.ObjectId
import org.corespring.container.client.hooks.{ PlayerHooks => ContainerPlayerHooks }
import org.corespring.container.client.integration.ContainerExecutionContext
import org.corespring.conversion.qti.transformers.ItemTransformer
import org.corespring.models.item.resource.StoredFile
import org.corespring.models.item.{ Item, PlayerDefinition }
import org.corespring.models.json.JsonFormatting
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.item.ItemService
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.auth.{ LoadOrgAndOptions, SessionAuth }
import org.corespring.v2.errors.Errors.{ cantParseItemId, generalError }
import org.corespring.v2.errors.V2Error
import org.corespring.v2.player.cdn.ItemAssetResolver
import play.api.Logger
import play.api.http.Status._
import play.api.libs.json._
import play.api.mvc._

import scala.concurrent.Future
import scala.util.matching.Regex
import scalaz.Scalaz._
import scalaz._

trait PlayerAssets {

  def loadItemFile(itemId: String, file: String)(implicit header: RequestHeader): SimpleResult

  def loadFile(id: String, path: String)(request: Request[AnyContent]): SimpleResult
}

class PlayerHooks(
  getOrgAndOptsFn: RequestHeader => Validation[V2Error, OrgAndOpts],
  itemAssetResolver: ItemAssetResolver,
  itemService: ItemService,
  itemTransformer: ItemTransformer,
  jsonFormatting: JsonFormatting,
  playerAssets: PlayerAssets,
  sessionAuth: SessionAuth[OrgAndOpts, PlayerDefinition],
  override implicit val containerContext: ContainerExecutionContext)
  extends ContainerPlayerHooks with LoadOrgAndOptions {

  implicit val formatPlayerDefinition = jsonFormatting.formatPlayerDefinition

  override def getOrgAndOptions(request: RequestHeader): Validation[V2Error, OrgAndOpts] = getOrgAndOptsFn.apply(request)

  lazy val logger = Logger(classOf[PlayerHooks])

  override def createSessionForItem(itemId: String)(implicit header: RequestHeader): Future[Either[(Int, String), (JsValue, JsValue)]] = Future {

    logger.debug(s"itemId=$itemId function=createSessionForItem")

    def createSessionJson(item: Item) = Json.obj(
      "itemId" -> item.id.toString,
      "collectionId" -> item.collectionId)

    lazy val getVid: Validation[V2Error, VersionedId[ObjectId]] = {
      val withVersion: Option[VersionedId[ObjectId]] = VersionedId(itemId).map {
        id =>
          lazy val currentVersion: Long = itemService.currentVersion(id)
          id.copy(version = id.version.orElse(Some(currentVersion)))
      }
      withVersion.toSuccess(cantParseItemId(itemId))
    }

    val result = for {
      identity <- getOrgAndOptions(header)
      canWrite <- sessionAuth.canCreate(itemId)(identity)
      writeAllowed <- if (canWrite) Success(true) else Failure(generalError(s"Can't create session for $itemId"))
      vid <- getVid
      item <- itemTransformer.loadItemAndUpdateV2(vid).toSuccess(generalError("Error generating item v2 JSON", INTERNAL_SERVER_ERROR))
      session <- Success(createSessionJson(item))
      sessionId <- sessionAuth.create(session)(identity)
      playerDefinitionJson <- Success(makePlayerDefinitionJson(session, item.playerDefinition))
    } yield (Json.obj("id" -> sessionId.toString) ++ session, playerDefinitionJson)

    result
      .leftMap(s => UNAUTHORIZED -> s.message)
      .toEither
  }

  override def loadSessionAndItem(sessionId: String)(implicit header: RequestHeader): Future[Either[(Int, String), (JsValue, JsValue)]] = Future {
    logger.debug(s"sessionId=$sessionId function=loadSessionAndItem")

    val o = for {
      identity <- getOrgAndOptions(header)
      models <- sessionAuth.loadForRead(sessionId)(identity)
    } yield models

    o.leftMap(s => s.statusCode -> s.message).rightMap { (models) =>
      val (session, playerDefinition) = models
      val playerDefinitionJson = makePlayerDefinitionJson(session, Some(playerDefinition))
      val withId: JsValue = Json.obj("id" -> sessionId) ++ session.as[JsObject]
      (withId, playerDefinitionJson)
    }.toEither
  }

  override def loadItemFile(itemId: String, file: String)(implicit header: RequestHeader): SimpleResult = playerAssets.loadItemFile(itemId, file)(header)

  override def loadFile(id: String, path: String)(request: Request[AnyContent]): SimpleResult = playerAssets.loadFile(id, path)(request)

  private def makePlayerDefinitionJson(session: JsValue, playerDefinition: Option[PlayerDefinition]): JsValue = {
    require(playerDefinition.isDefined, "playerDefinition cannot be empty")
    val storedFiles = playerDefinition.get.files.filter(_.isInstanceOf[StoredFile])
    val playerDefinitionJson = reducedPlayerDefinitionJson(playerDefinition.get)
    if (storedFiles.length == 0) {
      playerDefinitionJson
    } else {
      val maybeItemId = (session \ "itemId").asOpt[String]
      if (!maybeItemId.isDefined) {
        playerDefinitionJson
      } else {
        val resolve = itemAssetResolver.resolve(maybeItemId.get)_
        storedFiles.foldLeft(playerDefinitionJson) { (json, file) =>
          val fileMatcher = new Regex(file.name)
          val resolvedFile = resolve(file.name)
          def resolveFile(file: String): String = {
            fileMatcher.replaceAllIn(file, resolvedFile)
          }
          replaceStringsInJson(json, resolveFile);
        }
      }
    }
  }

  //Ensure that only the requested properties are returned
  private def reducedPlayerDefinitionJson(playerDefinition: PlayerDefinition): JsValue = {
    val v2Json = Json.toJson(playerDefinition)
    Json.obj(
      "xhtml" -> (v2Json \ "xhtml").as[String],
      "components" -> (v2Json \ "components").as[JsValue],
      "summaryFeedback" -> (v2Json \ "summaryFeedback").as[String])
  }

  private def replaceStringsInJson(json: JsValue, replace: (String => String)): JsValue = {
    def recurse(input: JsValue): JsValue = {
      if (input.isInstanceOf[JsObject]) {
        JsObject(input.as[JsObject].fields.map(kv => (kv._1, recurse(kv._2))))
      } else if (input.isInstanceOf[JsArray]) {
        JsArray(input.as[JsArray].value.map(v => recurse(v)))
      } else if (input.isInstanceOf[JsString]) {
        JsString(replace(input.as[JsString].value))
      } else {
        input
      }
    }
    recurse(json)
  }
}

