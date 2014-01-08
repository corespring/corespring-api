package org.corespring.poc.integration.impl

import _root_.securesocial.core.{SecureSocial, Identity}
import com.mongodb.casbah.MongoDB
import components.processing.PlayerItemPreProcessor
import org.bson.types.ObjectId
import org.corespring.amazon.s3.ConcreteS3Service
import org.corespring.container.client.controllers._
import org.corespring.container.components.model.Component
import org.corespring.container.components.outcome.{DefaultScoreProcessor, ScoreProcessor}
import org.corespring.container.components.processing.PlayerItemPreProcessorImpl
import org.corespring.container.components.response.{OutcomeProcessorImpl, OutcomeProcessor}
import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.core.models.Organization
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.models.itemSession.PreviewItemSessionCompanion
import org.corespring.platform.core.services.UserServiceImpl
import org.corespring.platform.core.services.item.{ItemServiceImpl, ItemService}
import org.corespring.poc.integration.impl.actionBuilders.{AuthenticatedSessionActionsImpl, AuthenticatedSessionActions}
import org.corespring.poc.integration.impl.controllers.editor.{ClientItemImpl, EditorHooksImpl}
import org.corespring.poc.integration.impl.controllers.player.{ClientSessionImpl, PlayerHooksImpl}
import org.corespring.poc.integration.impl.securesocial.SecureSocialService
import org.corespring.poc.integration.impl.transformers.ItemTransformer
import play.api.Configuration
import play.api.libs.json.JsValue
import play.api.mvc._
import scala.concurrent.Await
import play.api.mvc.SimpleResult
import org.corespring.container.components.model.UiComponent
import org.corespring.container.components.model.Library
import org.corespring.common.config.AppConfig
import org.corespring.platform.core.controllers.AssetResource
import scala.concurrent.duration._

class V2PlayerIntegration(comps: => Seq[Component], config: Configuration, db: MongoDB) extends AssetResource {

  private lazy val secureSocialServiceWrapper = new SecureSocialService {
    def currentUser(request: Request[AnyContent]): Option[Identity] = SecureSocial.currentUser(request)
  }

  def rootUiComponents = comps.filter(_.isInstanceOf[UiComponent]).map(_.asInstanceOf[UiComponent])

  def rootLibs = comps.filter(_.isInstanceOf[Library]).map(_.asInstanceOf[Library])

  def itemService: ItemService = ItemServiceImpl

  lazy val controllers: Seq[Controller] = Seq(playerHooks, editorHooks, items, sessions, assets, icons, rig, libs, playerLauncher)

  private lazy val playerLauncher: PlayerLauncher = new PlayerLauncher {
    //TODO: - plugin in secure mode
    def isSecure(r: Request[AnyContent]): Boolean = false
  }

  private lazy val rootSessionService: MongoService = new MongoService(db("v2.itemSessions"))

  private lazy val authenticatedSessionActions = new AuthenticatedSessionActionsImpl(
    ItemServiceImpl,
    Organization,
    rootSessionService,
    UserServiceImpl,
    secureSocialServiceWrapper)


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

    def getItemId(sessionId: String): Option[String] = PreviewItemSessionCompanion.findOneById(new ObjectId(sessionId)).map {
      s => s.itemId.toString()
    }
  }

  private lazy val playerHooks = new PlayerHooksImpl {

    def loadedComponents: Seq[Component] = comps

    def sessionService: MongoService = rootSessionService

    def itemService: ItemService = ItemServiceImpl

    def transformItem = ItemTransformer.transformToV2Json

    def auth: AuthenticatedSessionActions = authenticatedSessionActions
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

    def auth: AuthenticatedSessionActions = authenticatedSessionActions

    def itemPreProcessor: PlayerItemPreProcessor = new PlayerItemPreProcessorImpl(rootUiComponents, rootLibs)
  }

}
