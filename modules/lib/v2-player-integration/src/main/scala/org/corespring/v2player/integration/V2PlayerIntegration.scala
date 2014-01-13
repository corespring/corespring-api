package org.corespring.v2player.integration

import _root_.securesocial.core.{SecureSocial, Identity}
import com.mongodb.casbah.MongoDB
import org.bson.types.ObjectId
import org.corespring.amazon.s3.ConcreteS3Service
import org.corespring.container.client.controllers._
import org.corespring.container.components.model.Component
import org.corespring.container.components.outcome.{DefaultScoreProcessor, ScoreProcessor}
import org.corespring.container.components.processing.PlayerItemPreProcessor
import org.corespring.container.components.processing.rhino.{PlayerItemPreProcessor => RhinoPreProcessor}
import org.corespring.container.components.response.OutcomeProcessor
import org.corespring.container.components.response.rhino.{OutcomeProcessor => RhinoProcessor}
import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.core.models.Organization
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.models.itemSession.PreviewItemSessionCompanion
import org.corespring.platform.core.services.UserServiceWired
import org.corespring.platform.core.services.item.{ItemServiceWired, ItemService}
import org.corespring.v2player.integration.actionBuilders.access.PlayerOptions
import org.corespring.v2player.integration.actionBuilders.{AuthenticatedSessionActionsCheckUserAndPermissions, AuthenticatedSessionActions}
import org.corespring.v2player.integration.controllers.editor.{ItemWithBuilder, EditorHooksWithBuilder}
import org.corespring.v2player.integration.controllers.player.{ClientSessionWithBuilder, PlayerHooksWithBuilder}
import org.corespring.v2player.integration.securesocial.SecureSocialService
import org.corespring.v2player.integration.transformers.ItemTransformer
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
import scalaz.{Success, Validation}
import org.corespring.container.client.actions.{PlayerJsRequest, PlayerLauncherActionBuilder}

class V2PlayerIntegration(comps: => Seq[Component], config: Configuration, db: MongoDB) extends AssetResource {

  private lazy val secureSocialService = new SecureSocialService {
    def currentUser(request: Request[AnyContent]): Option[Identity] = SecureSocial.currentUser(request)
  }

  def rootUiComponents = comps.filter(_.isInstanceOf[UiComponent]).map(_.asInstanceOf[UiComponent])

  def rootLibs = comps.filter(_.isInstanceOf[Library]).map(_.asInstanceOf[Library])

  def itemService: ItemService = ItemServiceImpl

  lazy val controllers: Seq[Controller] = Seq(playerHooks, editorHooks, items, sessions, assets, icons, rig, libs, playerLauncher)

  private lazy val playerLauncher: PlayerLauncher = new PlayerLauncher {

    def builder: PlayerLauncherActionBuilder[AnyContent] = new PlayerLauncherActionBuilder[AnyContent] {
      //TODO: - handle decryption/secure mode etc.
      def playerJs(block: (PlayerJsRequest[AnyContent]) => Result): Action[AnyContent] = Action{request => block(new PlayerJsRequest(true, request))}
    }
  }

  private lazy val mainSessionService: MongoService = new MongoService(db("v2.itemSessions"))

  private lazy val authenticatedSessionActions = new AuthenticatedSessionActionsCheckUserAndPermissions(
    secureSocialService,
    UserServiceWired,
    mainSessionService,
    ItemServiceWired,
    Organization
  ) {
    def hasPermissions(sessionId: String, options: PlayerOptions): Validation[String, Boolean] = Success(true)
  }


  private lazy val icons = new Icons {
    def loadedComponents: Seq[Component] = comps
  }

  private lazy val rig = new Rig {
    override def uiComponents: Seq[UiComponent] = rootUiComponents

    protected def name: String = "rig"

    def loadedComponents: Seq[Component] =  comps
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

  private lazy val playerHooks = new PlayerHooksWithBuilder {

    def loadedComponents: Seq[Component] = comps

    def sessionService: MongoService = mainSessionService

    def itemService: ItemService = ItemServiceWired

    def transformItem = ItemTransformer.transformToV2Json

    def auth: AuthenticatedSessionActions = authenticatedSessionActions
  }

  private lazy val editorHooks = new EditorHooksWithBuilder {
    def loadedComponents: Seq[Component] = comps

    def itemService: ItemService = ItemServiceWired

    def transform: (Item) => JsValue = ItemTransformer.transformToV2Json
  }

  private lazy val items = new ItemWithBuilder {

    def scoreProcessor: ScoreProcessor = DefaultScoreProcessor

    def outcomeProcessor: OutcomeProcessor = new RhinoProcessor(rootUiComponents, rootLibs)

    def itemService: ItemService = ItemServiceWired

    def transform: (Item) => JsValue = ItemTransformer.transformToV2Json
  }

  private lazy val sessions = new ClientSessionWithBuilder {

    def outcomeProcessor: OutcomeProcessor = new RhinoProcessor(rootUiComponents, rootLibs)

    def scoreProcessor: ScoreProcessor = DefaultScoreProcessor

    def sessionService: MongoService = mainSessionService

    def itemService: ItemService = ItemServiceWired

    def transformItem: (Item) => JsValue = ItemTransformer.transformToV2Json

    def auth: AuthenticatedSessionActions = authenticatedSessionActions

    def itemPreProcessor: PlayerItemPreProcessor = new RhinoPreProcessor(rootUiComponents, rootLibs)
  }

}
