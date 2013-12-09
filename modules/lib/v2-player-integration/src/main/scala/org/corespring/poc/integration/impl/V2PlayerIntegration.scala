package org.corespring.poc.integration.impl

import org.corespring.container.components.model.{Library, UiComponent, Component}
import org.corespring.container.components.outcome.{DefaultScoreProcessor, ScoreProcessor}
import org.corespring.container.components.response.{OutcomeProcessorImpl, OutcomeProcessor}
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.models.itemSession.{PreviewItemSessionCompanion, ItemSessionCompanion}
import org.corespring.platform.core.services.item.{ItemServiceImpl, ItemService}
import org.corespring.poc.integration.impl.controllers.editor.{ClientItemImpl, EditorHooksImpl}
import org.corespring.poc.integration.impl.controllers.player.{ClientSessionImpl, PlayerHooksImpl}
import org.corespring.poc.integration.impl.transformers.{ItemSessionTransformer, ItemTransformer}
import play.api.libs.json.JsValue
import play.api.mvc.Controller

class V2PlayerIntegration(comps: => Seq[Component]) {

  def rootUiComponents = comps.filter(_.isInstanceOf[UiComponent]).map(_.asInstanceOf[UiComponent])

  def rootLibs = comps.filter(_.isInstanceOf[Library]).map(_.asInstanceOf[Library])

  lazy val controllers: Seq[Controller] = Seq(playerHooks, editorHooks, items, sessions)

  private lazy val playerHooks = new PlayerHooksImpl {

    def loadedComponents: Seq[Component] = comps

    def sessionService: ItemSessionCompanion = PreviewItemSessionCompanion
    def itemService: ItemService = ItemServiceImpl
    def transformItem = ItemTransformer.transformToPocItem
    def transformSession = ItemSessionTransformer.toPocJson
}

  private lazy val editorHooks = new EditorHooksImpl {
    def loadedComponents: Seq[Component] = comps

    def itemService: ItemService = ItemServiceImpl

    def transform: (Item) => JsValue = ItemTransformer.transformToPocItem
  }

  private lazy val items = new ClientItemImpl {

    def scoreProcessor: ScoreProcessor = DefaultScoreProcessor

    def outcomeProcessor: OutcomeProcessor = new OutcomeProcessorImpl(rootUiComponents, rootLibs)

    def itemService: ItemService = ItemServiceImpl

    def transform: (Item) => JsValue = ItemTransformer.transformToPocItem
  }

  private lazy val sessions = new ClientSessionImpl {

    def outcomeProcessor: OutcomeProcessor = new OutcomeProcessorImpl(rootUiComponents, rootLibs)

    def scoreProcessor: ScoreProcessor = DefaultScoreProcessor

    def sessionService: ItemSessionCompanion = PreviewItemSessionCompanion

    def itemService: ItemService = ItemServiceImpl

    def transformItem: (Item) => JsValue = ItemTransformer.transformToPocItem

    def sessionTransformer: ItemSessionTransformer = ItemSessionTransformer
  }
}
