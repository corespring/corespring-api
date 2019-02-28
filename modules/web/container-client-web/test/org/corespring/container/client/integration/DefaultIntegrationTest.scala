package org.corespring.container.client.integration

import java.net.URL

import org.corespring.container.client.hooks._
import org.corespring.container.client.pages.engine.JadeEngineConfig
import org.corespring.container.client.VersionInfo
import org.corespring.container.client.component.ComponentSetExecutionContext
import org.corespring.container.client.controllers.EditorConfig
import org.corespring.container.components.model.Component
import org.corespring.test.TestContext
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope
import play.api.Mode.Mode
import play.api.libs.json.{ JsObject, Json }
import play.api.mvc.{ AnyContent, Request }
import play.api.test.{ FakeApplication, FakeRequest, PlaySpecification }
import play.api.{ Configuration, GlobalSettings, Mode }

import scala.concurrent.ExecutionContext

class TestDI extends DefaultIntegration {

  override def containerContext: ContainerExecutionContext = ???

  override val loadResource: (String) => Option[URL] = _ => None

  override def containerConfig: ContainerConfig = ???

  override def collectionHooks: CollectionHooks = ???

  override def itemDraftSupportingMaterialHooks: ItemDraftSupportingMaterialHooks = ???

  override def sessionHooks: SessionHooks = ???

  override def itemDraftHooks: CoreItemHooks with DraftHooks = ???

  override def itemMetadataHooks: ItemMetadataHooks = ???

  override def itemHooks: CoreItemHooks with CreateItemHook = ???

  override def itemSupportingMaterialHooks: ItemSupportingMaterialHooks = ???

  override def playerLauncherHooks: PlayerLauncherHooks = ???

  override def componentSetExecutionContext: ComponentSetExecutionContext = ???

  override def playerHooks: PlayerHooks = ???

  override def catalogHooks: CatalogHooks = ???

  override def componentEditorHooks: ComponentEditorHooks = ???

  override def versionInfo: VersionInfo = ???

  override def dataQueryHooks: DataQueryHooks = ???

  override def draftEditorHooks: DraftEditorHooks = ???

  override def itemEditorHooks: ItemEditorHooks = ???

  override def components: Seq[Component] = ???

}

class DefaultIntegrationTest extends Specification with Mockito with PlaySpecification {

  def mkDefaultIntegration(json: JsObject) = {
    new DefaultIntegration with TestContext {

      override def versionInfo = VersionInfo("", "", "", "", Json.obj())

      override def playerHooks: PlayerHooks = mock[PlayerHooks]

      override def playerLauncherHooks: PlayerLauncherHooks = mock[PlayerLauncherHooks]

      override def catalogHooks: CatalogHooks = mock[CatalogHooks]

      override def componentEditorHooks: ComponentEditorHooks = mock[ComponentEditorHooks]

      override def draftEditorHooks: DraftEditorHooks = mock[DraftEditorHooks]

      override def itemEditorHooks: ItemEditorHooks = mock[ItemEditorHooks]

      override def dataQueryHooks: DataQueryHooks = mock[DataQueryHooks]

      override def itemMetadataHooks: ItemMetadataHooks = mock[ItemMetadataHooks]

      override def sessionHooks: SessionHooks = {
        val m = mock[SessionHooks]
        m.loadItemAndSession(anyString)(any[Request[AnyContent]]) returns Right(FullSession(json, false))
        m
      }

      override def resolveDomain(s: String) = s

      override def collectionHooks: CollectionHooks = mock[CollectionHooks]

      override def itemDraftHooks: CoreItemHooks with DraftHooks = mock[CoreItemHooks with DraftHooks]

      override def components: Seq[Component] = Seq.empty

      override def itemHooks: CoreItemHooks with CreateItemHook = mock[CoreItemHooks with CreateItemHook]

      override def itemDraftSupportingMaterialHooks: ItemDraftSupportingMaterialHooks = mock[ItemDraftSupportingMaterialHooks]

      override def itemSupportingMaterialHooks: ItemSupportingMaterialHooks = mock[ItemSupportingMaterialHooks]

      override def jadeEngineConfig: JadeEngineConfig = mock[JadeEngineConfig]

      override val loadResource: (String) => Option[URL] = _ => None

      override def containerConfig: ContainerConfig = mock[ContainerConfig]

      override def componentSetExecutionContext: ComponentSetExecutionContext = ComponentSetExecutionContext(ExecutionContext.global)
    }
  }

  "assetPathProcessor" should {

    trait scope extends Scope {
      val spied = spy[DefaultIntegration](new TestDI())
    }

    "call revolveDomain" in new scope {

      forall(DefaultIntegration.pathsThatNeedResolution) { k =>
        spied.assetPathProcessor.process(k)
        there was one(spied).resolveDomain(k)
      }
    }
  }

  "session.loadItemAndSession" should {

    "the loaded session has no correctResponse or feedback" in {

      val mockJson = Json.obj(
        "item" -> Json.obj(
          "components" -> Json.obj(
            "0" -> Json.obj(
              "componentType" -> "type",
              "correctResponse" -> Json.obj("a" -> "b"),
              "feedback" -> Json.obj("fb" -> "A was right")))), "session" -> Json.obj())

      val di = mkDefaultIntegration(mockJson)

      val result = di.session.loadItemAndSession("id")(FakeRequest("", ""))

      val json = contentAsJson(result)

      (json \ "item" \ "components" \ "0" \ "correctResponse").asOpt[JsObject] === None
      (json \ "item" \ "components" \ "0" \ "feedback").asOpt[JsObject] === None
      (json \ "item" \ "components" \ "0" \ "componentType").asOpt[String] === Some("type")

    }
  }
}
