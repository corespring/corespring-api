package org.corespring.v2.player

import org.corespring.amazon.s3.S3Service
import org.corespring.container.client.{ VersionInfo }
import org.corespring.container.client.integration.{ ContainerExecutionContext, DefaultIntegration }
import org.corespring.container.components.loader.ComponentLoader
import org.corespring.container.components.model.Component
import org.corespring.drafts.item.ItemDrafts
import org.corespring.models.appConfig.Bucket
import org.corespring.models.item.PlayerDefinition
import org.corespring.models.json.JsonFormatting
import org.corespring.qtiToV2.transformers.ItemTransformer
import org.corespring.services._
import org.corespring.services.item.ItemService
import org.corespring.v2.auth.{ SessionAuth, ItemAuth }
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.V2Error
import org.corespring.v2.player.assets.{ PlayerAssetHelper, CatalogAssetHelper }
import org.corespring.v2.player.hooks._
import org.corespring.container.client
import org.corespring.v2.sessiondb.SessionServices
import play.api.Mode.Mode
import play.api.libs.json.JsObject
import play.api.mvc.RequestHeader

import scala.concurrent.ExecutionContext
import scalaz.Validation

case class AllComponents(components: Seq[Component])

class V2PlayerExecutionContext(underlying: ExecutionContext) extends ExecutionContext {
  override def execute(runnable: Runnable): Unit = underlying.execute(runnable)

  override def reportFailure(t: Throwable): Unit = underlying.reportFailure(t)
}

trait V2PlayerModule extends DefaultIntegration {

  import com.softwaremill.macwire.MacwireMacros._

  def playMode: Mode

  def itemService: ItemService
  def orgService: OrganizationService
  def subjectService: SubjectService
  def standardService: StandardService
  def contentCollectionService: ContentCollectionService
  def userService: UserService
  def s3Service: S3Service
  def itemDrafts: ItemDrafts

  def itemAuth: ItemAuth[OrgAndOpts]
  def sessionAuth: SessionAuth[OrgAndOpts, PlayerDefinition]

  def itemTransformer: ItemTransformer
  def getOrgAndOptsFn: RequestHeader => Validation[V2Error, OrgAndOpts]

  def standardTree: StandardsTree
  def jsonFormatting: JsonFormatting

  def bucket: Bucket

  def sessionServices: SessionServices

  lazy val catalogAssets: CatalogAssets = wire[CatalogAssetHelper]
  lazy val playerAssets: PlayerAssets = wire[PlayerAssetHelper]

  def componentLoader: ComponentLoader

  override def components: Seq[Component] = componentLoader.all

  override lazy val versionInfo: JsObject = VersionInfo(configuration)

  override lazy val draftEditorHooks: client.hooks.DraftEditorHooks = wire[DraftEditorHooks]

  override lazy val itemEditorHooks: client.hooks.ItemEditorHooks = wire[ItemEditorHooks]

  override lazy val playerHooks: client.hooks.PlayerHooks = wire[PlayerHooks]
  override lazy val catalogHooks: client.hooks.CatalogHooks = wire[CatalogHooks]

  override lazy val collectionHooks: client.hooks.CollectionHooks = wire[CollectionHooks]

  override lazy val playerLauncherHooks: client.hooks.PlayerLauncherHooks = wire[PlayerLauncherHooks]

  override lazy val itemDraftHooks: client.hooks.DraftHooks = wire[ItemDraftHooks]

  override lazy val itemHooks: client.hooks.ItemHooks = wire[ItemHooks]

  override lazy val sessionHooks: client.hooks.SessionHooks = wire[SessionHooks]

  override lazy val dataQueryHooks: client.hooks.DataQueryHooks = wire[DataQueryHooks]

  override lazy val componentSets: client.controllers.ComponentSets = wire[CompressedComponentSets]
}
