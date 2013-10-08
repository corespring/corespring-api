package org.corespring.poc.integration.impl

import org.corespring.container.components.model.Component
import org.corespring.container.components.outcome.{DefaultOutcomeProcessor, OutcomeProcessor}
import org.corespring.container.components.response.{ResponseProcessorImpl, ResponseProcessor}
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.models.itemSession.{PreviewItemSessionCompanion, ItemSessionCompanion, ItemSession}
import org.corespring.platform.core.services.item.{ItemServiceImpl, ItemService}
import org.corespring.poc.integration.impl.controllers.editor.{ClientItemImpl, EditorHooksImpl}
import org.corespring.poc.integration.impl.controllers.player.{ClientSessionImpl, PlayerHooksImpl}
import org.corespring.poc.integration.impl.transformers.{ItemSessionTransformer, ItemTransformer}
import play.api.libs.json.JsValue
import play.api.mvc.Controller

class PocIntegrationImpl(comps: => Seq[Component]) {

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
    def itemService: ItemService = ItemServiceImpl

    def transform: (Item) => JsValue = ItemTransformer.transformToPocItem
  }

  private lazy val sessions = new ClientSessionImpl {

    def responseProcessor: ResponseProcessor = new ResponseProcessorImpl(comps)

    def outcomeProcessor: OutcomeProcessor = DefaultOutcomeProcessor

    def sessionService: ItemSessionCompanion = PreviewItemSessionCompanion

    def itemService: ItemService = ItemServiceImpl

    def transformItem: (Item) => JsValue = ItemTransformer.transformToPocItem

    def sessionTransformer: ItemSessionTransformer = ItemSessionTransformer
  }
}
