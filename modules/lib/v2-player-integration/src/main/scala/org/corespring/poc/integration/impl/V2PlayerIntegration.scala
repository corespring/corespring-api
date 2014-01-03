package org.corespring.poc.integration.impl

import com.mongodb.casbah.MongoDB
import org.bson.types.ObjectId
import org.corespring.amazon.s3.ConcreteS3Service
import org.corespring.container.client.controllers.{ComponentsFileController, Rig, Icons, Assets}
import org.corespring.container.components.model.Component
import org.corespring.container.components.model.Library
import org.corespring.container.components.model.UiComponent
import org.corespring.container.components.outcome.{DefaultScoreProcessor, ScoreProcessor}
import org.corespring.container.components.response.{OutcomeProcessorImpl, OutcomeProcessor}
import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.models.itemSession.{DefaultItemSession, PreviewItemSessionCompanion}
import org.corespring.platform.core.services.item.{ItemServiceImpl, ItemService}
import org.corespring.poc.integration.impl.controllers.editor.{ClientItemImpl, EditorHooksImpl}
import org.corespring.poc.integration.impl.controllers.player.{ClientSessionImpl, PlayerHooksImpl}
import org.corespring.poc.integration.impl.transformers.ItemTransformer
import play.api.Configuration
import play.api.libs.json.{JsObject, JsValue}
import play.api.mvc._
import scala.Some
import scala.concurrent.{Await, ExecutionContext, Future}
import play.api.mvc.Results._
import scala.Some
import play.api.mvc.SimpleResult
import org.corespring.container.components.model.UiComponent
import org.corespring.container.components.model.Library
import org.corespring.common.config.AppConfig
import org.corespring.platform.core.controllers.AssetResource
import scala.concurrent.duration._


class V2PlayerIntegration(comps: => Seq[Component], config: Configuration, db : MongoDB) extends AssetResource {

  def rootUiComponents = comps.filter(_.isInstanceOf[UiComponent]).map(_.asInstanceOf[UiComponent])

  def rootLibs = comps.filter(_.isInstanceOf[Library]).map(_.asInstanceOf[Library])

  def itemService: ItemService = ItemServiceImpl

  lazy val controllers: Seq[Controller] = Seq(playerHooks, editorHooks, items, sessions, assets, icons, rig, libs)

  private lazy val rootSessionService : MongoService = new MongoService(db("v2.itemSessions"))

  private lazy val icons = new Icons {
    def loadedComponents: Seq[Component] = comps
  }

  private lazy val rig = new Rig {
    def uiComponents: Seq[UiComponent] = rootUiComponents
  }

  private lazy val libs = new ComponentsFileController {
    def componentsPath: String = config.getString("components.path").getOrElse("components")
    def defaultCharSet: String = config.getString("default.charset").getOrElse("utf-8")
  }

  private lazy val assets = new Assets {

    private lazy val key = AppConfig.amazonKey
    private lazy val secret = AppConfig.amazonSecret
    private lazy val bucket = AppConfig.assetsBucket

    lazy val playS3 = new ConcreteS3Service(key, secret)

    def loadAsset(id: String, file: String)(request: Request[AnyContent]): SimpleResult = {
      Await.result(getDataFile(id, file)(request), Duration(3000, SECONDS))
    }

    //TODO: Need to look at a way of pre-validating before we upload - look at the predicate?
    def uploadBodyParser(id: String, file: String): BodyParser[Int] = playS3.upload(bucket, s"$id/$file", (rh) => None)

    def getItemId(sessionId: String): Option[String] = rootSessionService.load(sessionId) match {
      case Some(json: JsObject) => {
        (json \ "itemId").asOpt[String] match {
          case Some(itemId) => Some(itemId.split(":").head)
          case _ => None
        }
      }
      case _ => None
    }
  }

  private lazy val playerHooks = new PlayerHooksImpl {

    def loadedComponents: Seq[Component] = comps

    def sessionService: MongoService = rootSessionService
    def itemService: ItemService = ItemServiceImpl
    def transformItem = ItemTransformer.transformToV2Json
}

  private lazy val editorHooks = new EditorHooksImpl {
    def loadedComponents: Seq[Component] = comps

    def itemService: ItemService = ItemServiceImpl

    def transform: (Item) => JsValue = ItemTransformer.transformToV2Json
  }

  private lazy val items = new ClientItemImpl {

    def scoreProcessor: ScoreProcessor = DefaultScoreProcessor

    def outcomeProcessor: OutcomeProcessor = new OutcomeProcessorImpl(rootUiComponents, rootLibs)

    def itemService: ItemService = ItemServiceImpl

    def transform: (Item) => JsValue = ItemTransformer.transformToV2Json
  }

  private lazy val sessions = new ClientSessionImpl {

    def outcomeProcessor: OutcomeProcessor = new OutcomeProcessorImpl(rootUiComponents, rootLibs)

    def scoreProcessor: ScoreProcessor = DefaultScoreProcessor

    def sessionService: MongoService = rootSessionService

    def itemService: ItemService = ItemServiceImpl

    def transformItem: (Item) => JsValue = ItemTransformer.transformToV2Json

  }

}
