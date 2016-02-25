package org.corespring.v2.player

import java.net.URL

import org.corespring.amazon.s3.S3Service
import org.corespring.common.config.ContainerConfig
import org.corespring.container.client.controllers.apps.PageSourceServiceConfig
import org.corespring.container.client.controllers.resources.SessionExecutionContext
import org.corespring.container.client.io.ResourcePath
import org.corespring.container.client.pages.engine.JadeEngineConfig
import org.corespring.container.client.{ ComponentSetExecutionContext, VersionInfo }
import org.corespring.container.client.integration.{ ContainerExecutionContext, DefaultIntegration }
import org.corespring.container.components.loader.ComponentLoader
import org.corespring.container.components.model.Component
import org.corespring.conversion.qti.transformers.{ PlayerJsonToItem, ItemTransformer }
import org.corespring.drafts.item.ItemDrafts
import org.corespring.models.appConfig.{ ArchiveConfig, Bucket }
import org.corespring.models.item.PlayerDefinition
import org.corespring.models.json.JsonFormatting
import org.corespring.services._
import org.corespring.services.item.ItemService
import org.corespring.services.metadata.MetadataService
import org.corespring.services.metadata.MetadataSetService
import org.corespring.v2.auth.{ SessionAuth, ItemAuth }
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.V2Error
import org.corespring.v2.player.assets.{ PlayerAssetHelper, CatalogAssetHelper }
import org.corespring.v2.player.cdn.ItemAssetResolver
import org.corespring.v2.player.hooks._
import org.corespring.container.client
import org.corespring.v2.player.services.item.{ DraftSupportingMaterialsService, ItemSupportingMaterialsService }
import org.corespring.v2.sessiondb.SessionServices
import play.api.Configuration
import play.api.Mode.Mode
import play.api.libs.json.JsObject
import play.api.mvc.RequestHeader

import scala.concurrent.ExecutionContext
import scalaz.Validation

case class AllComponents(components: Seq[Component])

case class V2PlayerExecutionContext(underlying: ExecutionContext) extends ExecutionContext {
  override def execute(runnable: Runnable): Unit = underlying.execute(runnable)

  override def reportFailure(t: Throwable): Unit = underlying.reportFailure(t)
}

trait V2PlayerModule extends DefaultIntegration {

  import com.softwaremill.macwire.MacwireMacros._

  def archiveConfig: ArchiveConfig
  def bucket: Bucket
  def componentLoader: ComponentLoader
  def componentSetExecutionContext: ComponentSetExecutionContext
  def containerConfig: ContainerConfig
  def contentCollectionService: ContentCollectionService
  def draftSupportingMaterialsService: DraftSupportingMaterialsService
  def getOrgAndOptsFn: RequestHeader => Validation[V2Error, OrgAndOpts]
  def itemAssetResolver: ItemAssetResolver
  def itemAuth: ItemAuth[OrgAndOpts]
  def itemDrafts: ItemDrafts
  def itemService: ItemService
  def itemSupportingMaterialsService: ItemSupportingMaterialsService
  def itemTransformer: ItemTransformer
  def jsonFormatting: JsonFormatting
  def metadataService: MetadataService
  def metadataSetService: MetadataSetService
  def orgCollectionService: OrgCollectionService
  def orgService: OrganizationService
  def playerJsonToItem: PlayerJsonToItem
  def playMode: Mode

  def s3Service: S3Service
  def sessionAuth: SessionAuth[OrgAndOpts, PlayerDefinition]
  def sessionExecutionContext: SessionExecutionContext
  def sessionServices: SessionServices
  def standardService: StandardService
  def standardTree: StandardsTree
  def subjectService: SubjectService
  def userService: UserService
  def v2PlayerExecutionContext: V2PlayerExecutionContext

  lazy val catalogAssets: CatalogAssets = wire[CatalogAssetHelper]
  lazy val playerAssets: PlayerAssets = wire[PlayerAssetHelper]
  lazy val playerItemProcessor: PlayerItemProcessor = wire[CdnPlayerItemProcessor]

  override def components: Seq[Component] = componentLoader.all

  override lazy val itemDraftSupportingMaterialHooks: client.hooks.ItemDraftSupportingMaterialHooks = {
    wire[ItemDraftSupportingMaterialHooks]
  }

  override lazy val itemSupportingMaterialHooks: client.hooks.ItemSupportingMaterialHooks = {
    wire[ItemSupportingMaterialHooks]
  }

  override lazy val catalogHooks: client.hooks.CatalogHooks = wire[CatalogHooks]
  override lazy val collectionHooks: client.hooks.CollectionHooks = wire[CollectionHooks]
  override lazy val componentSets: client.controllers.ComponentSets = wire[CompressedComponentSets]
  override lazy val dataQueryHooks: client.hooks.DataQueryHooks = wire[DataQueryHooks]
  override lazy val draftEditorHooks: client.hooks.DraftEditorHooks = wire[DraftEditorHooks]
  override lazy val itemDraftHooks: client.hooks.DraftHooks with client.hooks.CoreItemHooks = wire[ItemDraftHooks]
  override lazy val itemEditorHooks: client.hooks.ItemEditorHooks = wire[ItemEditorHooks]
  override lazy val itemHooks: client.hooks.ItemHooks = wire[ItemHooks]
  override lazy val itemMetadataHooks: client.hooks.ItemMetadataHooks = wire[ItemMetadataHooks]
  override lazy val playerHooks: client.hooks.PlayerHooks = wire[PlayerHooks]
  override lazy val playerLauncherHooks: client.hooks.PlayerLauncherHooks = wire[PlayerLauncherHooks]
  override lazy val sessionHooks: client.hooks.SessionHooks = wire[SessionHooks]
  override lazy val versionInfo: JsObject = VersionInfo(containerConfig.config)

}
