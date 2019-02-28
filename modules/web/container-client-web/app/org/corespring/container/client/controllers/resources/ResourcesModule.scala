package org.corespring.container.client.controllers.resources

import com.softwaremill.macwire.MacwireMacros.wire
import org.corespring.container.client.controllers.helpers.{ ItemInspector, PlayerXhtml, XhtmlProcessor, XmlItemInspector }
import org.corespring.container.client.hooks._
import org.corespring.container.client.integration.ContainerExecutionContext
import org.corespring.container.components.outcome.ScoreProcessor
import org.corespring.container.components.processing.PlayerItemPreProcessor
import org.corespring.container.components.response.OutcomeProcessor
import org.corespring.container.components.services.ComponentService

trait ResourcesModule {

  def outcomeProcessor: OutcomeProcessor
  def scoreProcessor: ScoreProcessor
  def itemSupportingMaterialHooks: ItemSupportingMaterialHooks
  def itemDraftSupportingMaterialHooks: ItemDraftSupportingMaterialHooks

  def containerContext: ContainerExecutionContext

  def sessionExecutionContext: SessionExecutionContext
  def playerItemPreProcessor: PlayerItemPreProcessor
  def componentService: ComponentService
  def playerXhtml: PlayerXhtml

  def sessionHooks: SessionHooks
  def itemHooks: CoreItemHooks with CreateItemHook
  def itemDraftHooks: CoreItemHooks with DraftHooks
  def itemMetadataHooks: ItemMetadataHooks
  def collectionHooks: CollectionHooks

  lazy val itemInspector: ItemInspector = new XmlItemInspector(XhtmlProcessor, containerContext.context)

  lazy val session: Session = wire[Session]
  lazy val item: Item = wire[Item]
  lazy val itemDraft: ItemDraft = wire[ItemDraft]
  lazy val itemMetadata: ItemMetadata = wire[ItemMetadata]
  lazy val collection: Collection = wire[Collection]

  lazy val resourceControllers = Seq(
    session,
    item,
    itemDraft,
    itemMetadata,
    collection)
}
