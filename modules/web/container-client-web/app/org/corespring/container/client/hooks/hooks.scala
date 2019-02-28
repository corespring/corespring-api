package org.corespring.container.client.hooks

import java.io.InputStream

import org.corespring.container.client.HasContainerContext
import org.corespring.container.client.hooks.Hooks.{ ItemAndDefaults, ItemAndSessionAndDefaults, R, StatusMessage }
import play.api.libs.json.{ JsArray, JsObject, JsValue }
import play.api.mvc._

import scala.concurrent.Future

object Hooks {
  type StatusMessage = (Int, String)
  type ItemAndDefaults = (JsValue, JsValue)
  type ItemAndSessionAndDefaults = (JsValue, JsValue, JsValue)
  type R[A] = Future[Either[StatusMessage, A]]
}

trait GetAssetHook {
  def loadFile(id: String, path: String)(request: Request[AnyContent]): SimpleResult
}

case class UploadResult(path: String)

trait AssetHooks extends GetAssetHook {
  def deleteFile(id: String, file: String)(implicit header: RequestHeader): Future[Option[StatusMessage]]
  def upload(id: String, file: String)(predicate: RequestHeader => Option[SimpleResult]): BodyParser[Future[UploadResult]]
}

/**
 * Client side calls - each will call for config, services and components
 */
trait LoadHook extends HasContainerContext {

  /**
   * load the item with the id into the editor, aka it will be read+write access.
   * If returning a status message - you can optionally return a SEE_OTHER status and it will be handled correctly
   *
   * @param id
   * @param header
   * @return
   */
  def load(id: String)(implicit header: RequestHeader): Future[Either[StatusMessage, ItemAndDefaults]]
}

trait PlayerHooks extends GetAssetHook with HasContainerContext {
  def createSessionForItem(itemId: String)(implicit header: RequestHeader): Future[Either[StatusMessage, ItemAndSessionAndDefaults]]
  def loadSessionAndItem(sessionId: String)(implicit header: RequestHeader): Future[Either[StatusMessage, ItemAndSessionAndDefaults]]
  def loadItemFile(itemId: String, file: String)(implicit header: RequestHeader): SimpleResult
  def archiveCollectionId: String
}

trait CatalogHooks extends LoadHook with GetAssetHook {
  def showCatalog(itemId: String)(implicit header: RequestHeader): Future[Either[StatusMessage, JsValue]]
}

trait ComponentEditorHooks extends LoadHook
trait EditorHooks extends LoadHook with AssetHooks
trait DraftEditorHooks extends EditorHooks
trait ItemEditorHooks extends EditorHooks

case class FileDataStream(stream: InputStream, contentLength: Long, contentType: String, metadata: Map[String, String])

trait SupportingMaterialHooks {
  /**
   * Return the resource json
   * Note: once we refactor cs-api, we should have api data models available.
   * So can use these instead of JsValue
   */
  def create[F <: File](id: String, sm: CreateNewMaterialRequest[F])(implicit h: RequestHeader): R[JsValue]
  def delete(id: String, name: String)(implicit h: RequestHeader): R[JsValue]
  def addAsset(id: String, name: String, binary: Binary)(implicit h: RequestHeader): R[UploadResult]
  def deleteAsset(id: String, name: String, filename: String)(implicit h: RequestHeader): R[JsValue]
  def getAsset(id: String, name: String, filename: String)(implicit h: RequestHeader): Future[Either[StatusMessage, FileDataStream]]
  def updateContent(id: String, name: String, filename: String, content: String)(implicit h: RequestHeader): R[JsValue]
}

trait ItemSupportingMaterialHooks extends SupportingMaterialHooks
trait ItemDraftSupportingMaterialHooks extends SupportingMaterialHooks

trait CoreItemHooks extends HasContainerContext with LoadHook {
  def delete(id: String)(implicit h: RequestHeader): R[JsValue]
  def saveCollectionId(id: String, collectionId: String)(implicit h: RequestHeader): R[JsValue]
  def saveComponents(id: String, json: JsValue)(implicit h: RequestHeader): R[JsValue]
  def saveConfig(id: String, json: JsValue)(implicit h: RequestHeader): R[JsValue]
  def saveConfigXhtmlAndComponents(id: String, config: JsValue, markup: String, components: JsValue)(implicit h: RequestHeader): R[JsValue]
  def saveCustomScoring(id: String, customScoring: String)(implicit header: RequestHeader): R[JsValue]
  def saveProfile(id: String, json: JsValue)(implicit h: RequestHeader): R[JsValue]
  def saveSummaryFeedback(id: String, feedback: String)(implicit h: RequestHeader): R[JsValue]
  def saveSupportingMaterials(id: String, json: JsValue)(implicit h: RequestHeader): R[JsValue]
  def saveXhtml(id: String, xhtml: String)(implicit h: RequestHeader): R[JsValue]
}

trait DraftHooks {
  def save(draftId: String, json: JsValue)(implicit request: RequestHeader): R[JsValue]
  def createItemAndDraft(collectionId: Option[String])(implicit h: RequestHeader): R[(String, String)]
  def createSingleComponentItemDraft(collectionId: Option[String], componentType: String, key: String, defaultData: JsObject)(implicit r: RequestHeader): R[(String, String)]
  def commit(id: String, force: Boolean)(implicit h: RequestHeader): R[JsValue]
}

trait CreateItemHook {
  def createItem(collectionId: Option[String])(implicit h: RequestHeader): R[String]
  def createSingleComponentItem(collectionId: Option[String], componentType: String, key: String, defaultData: JsObject)(implicit h: RequestHeader): R[String]
}

trait ItemHooks extends CoreItemHooks with CreateItemHook

trait SessionHooks extends HasContainerContext {
  def getScore(id: String)(implicit header: RequestHeader): Either[StatusMessage, SessionOutcome]
  def load(id: String)(implicit header: RequestHeader): Future[Either[StatusMessage, JsValue]]
  def loadItemAndSession(sessionId: String)(implicit header: RequestHeader): Either[StatusMessage, FullSession]
  def loadOutcome(id: String)(implicit header: RequestHeader): Either[StatusMessage, SessionOutcome]
  def save(id: String)(implicit header: RequestHeader): Future[Either[StatusMessage, SaveSession]]
}

trait PlayerLauncherHooks extends HasContainerContext {
  def catalogJs(implicit header: RequestHeader): Future[PlayerJs]
  def editorJs(implicit header: RequestHeader): Future[PlayerJs]
  def componentEditorJs(implicit header: RequestHeader): Future[PlayerJs]
  def playerJs(implicit header: RequestHeader): Future[PlayerJs]
}

trait DataQueryHooks extends HasContainerContext {
  def list(topic: String, query: Option[String] = None)(implicit header: RequestHeader): Future[Either[StatusMessage, JsArray]]

  def findOne(topic: String, id: String)(implicit header: RequestHeader): Future[Either[StatusMessage, Option[JsValue]]]
}

trait CollectionHooks extends HasContainerContext {
  def list()(implicit header: RequestHeader): Future[Either[StatusMessage, JsArray]]
}

trait ItemMetadataHooks extends HasContainerContext {
  def get(id: String)(implicit header: RequestHeader): Future[Either[StatusMessage, JsValue]]
}

