package org.corespring.v2player.integration

import _root_.securesocial.core.{ SecureSocial, Identity }
import com.mongodb.casbah.MongoDB
import org.corespring.amazon.s3.ConcreteS3Service
import org.corespring.common.config.AppConfig
import org.corespring.container.client.component.{ PlayerGenerator, EditorGenerator, SourceGenerator, ComponentUrls }
import org.corespring.container.client.controllers._
import org.corespring.container.components.model.Component
import org.corespring.container.components.model.Library
import org.corespring.container.components.model.UiComponent
import org.corespring.container.components.outcome.{ DefaultScoreProcessor, ScoreProcessor }
import org.corespring.container.components.processing.PlayerItemPreProcessor
import org.corespring.container.js.processing.rhino.{ PlayerItemPreProcessor => RhinoPreProcessor }
import org.corespring.container.components.response.OutcomeProcessor
import org.corespring.container.js.response.rhino.{ OutcomeProcessor => RhinoProcessor }
import org.corespring.dev.tools.DevTools
import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.core.models.Organization
import org.corespring.platform.core.models.item.Item
import org.corespring.platform.core.models.item.resource.StoredFile
import org.corespring.platform.core.services.item.{ ItemServiceWired, ItemService }
import org.corespring.platform.core.services.organization.OrganizationService
import org.corespring.platform.core.services.{ UserService, UserServiceWired }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2player.integration.actionBuilders._
import org.corespring.v2player.integration.actionBuilders.access.PlayerOptions
import org.corespring.v2player.integration.actionBuilders.permissions.SimpleWildcardChecker
import org.corespring.v2player.integration.controllers.DefaultPlayerLauncherActions
import org.corespring.v2player.integration.controllers.editor._
import org.corespring.v2player.integration.controllers.player.{ SessionActions, PlayerActions }
import org.corespring.v2player.integration.securesocial.SecureSocialService
import org.corespring.v2player.integration.transformers.ItemTransformer
import play.api.cache.Cached
import play.api.libs.json.JsValue
import play.api.mvc._
import play.api.{ Mode, Play, Logger, Configuration }
import scala.Some
import scalaz.Failure
import scalaz.Success
import scalaz.Validation
import org.corespring.v2player.integration.actionBuilders.access.Mode.Mode
import org.corespring.container.client.actions
import org.bson.types.ObjectId
import org.corespring.common.encryption.{ NullCrypto, AESCrypto }
import org.corespring.platform.core.encryption.OrgEncrypter
import org.corespring.platform.core.models.auth.ApiClient
import scalaz.Failure
import scala.Some
import play.api.mvc.SimpleResult
import scalaz.Success
import org.corespring.container.components.model.Library
import org.corespring.container.components.model.UiComponent

class V2PlayerIntegration(comps: => Seq[Component], rootConfig: Configuration, db: MongoDB)
  extends org.corespring.container.client.integration.DefaultIntegration {

  lazy val logger = Logger("v2player.integration")

  private lazy val mainSecureSocialService = new SecureSocialService {
    def currentUser(request: Request[AnyContent]): Option[Identity] = SecureSocial.currentUser(request)
  }

  def rootUiComponents = comps.filter(_.isInstanceOf[UiComponent]).map(_.asInstanceOf[UiComponent])

  def rootLibs = comps.filter(_.isInstanceOf[Library]).map(_.asInstanceOf[Library])

  def itemService: ItemService = ItemServiceWired

  private lazy val playerActions: PlayerLauncherActions = new DefaultPlayerLauncherActions(
    mainSecureSocialService,
    UserServiceWired,
    rootConfig)

  private lazy val mainSessionService: MongoService = new MongoService(db("v2.itemSessions"))

  private lazy val authActions = new AuthSessionActionsCheckPermissions(
    mainSecureSocialService,
    UserServiceWired,
    mainSessionService,
    ItemServiceWired,
    Organization) {

    val permissionGranter = new SimpleWildcardChecker()

    override def hasPermissions(itemId: String, sessionId: Option[String], mode: Mode, options: PlayerOptions): Validation[String, Boolean] = permissionGranter.allow(itemId, sessionId, mode, options) match {
      case Left(error) => Failure(error)
      case Right(allowed) => Success(true)
    }
  }

  private lazy val authenticatedSessionActions = if (DevTools.enabled) {
    new DevToolsSessionActions(authActions)
  } else {
    authActions
  }

  /*private lazy val icons = new Icons {
    def loadedComponents: Seq[Component] = comps
  }*/

  /*private lazy val rig = new Rig {

    override def urls: ComponentUrls = componentSets

    override def components: Seq[Component] = comps
  }*/

  /*private lazy val libs = new ComponentsFileController {
    def componentsPath: String = rootConfig.getString("components.path").getOrElse("components")

    def defaultCharSet: String = rootConfig.getString("default.charset").getOrElse("utf-8")
  }*/

  lazy val assets = new Assets {

    private lazy val key = AppConfig.amazonKey
    private lazy val secret = AppConfig.amazonSecret
    private lazy val bucket = AppConfig.assetsBucket

    lazy val playS3 = new ConcreteS3Service(key, secret)

    def loadAsset(itemId: String, file: String)(request: Request[AnyContent]): SimpleResult = {

      import scalaz.Scalaz._
      import scalaz._

      val decodedFilename = java.net.URI.create(file).getPath
      val storedFile: Validation[String, StoredFile] = for {
        vid <- VersionedId(itemId).toSuccess(s"invalid item id: $itemId")
        item <- ItemServiceWired.findOneById(vid).toSuccess(s"can't find item with id: $vid")
        data <- item.data.toSuccess(s"item doesn't contain a 'data' property': $vid")
        asset <- data.files.find(_.name == decodedFilename).toSuccess(s"can't find a file with name: $decodedFilename in ${data}")
      } yield asset.asInstanceOf[StoredFile]

      storedFile match {
        case Success(sf) => {
          logger.debug(s"loadAsset: itemId: $itemId -> file: $file")
          playS3.download(bucket, sf.storageKey, Some(request.headers))
        }
        case Failure(msg) => {
          logger.warn(s"can't load file: $msg")
          NotFound(msg)
        }
      }
    }

    //TODO: Need to look at a way of pre-validating before we upload - look at the predicate?
    def uploadBodyParser(id: String, file: String): BodyParser[Int] = playS3.upload(bucket, s"$id/$file", (rh) => None)

    def getItemId(sessionId: String): Option[String] = mainSessionService.load(sessionId).map {
      s => (s \ "itemId").as[String]
    }
  }

  lazy val componentUrls: ComponentSets = new ComponentSets {

    override def allComponents: Seq[Component] = comps
    override def resource[A >: play.api.mvc.EssentialAction](context: scala.Predef.String, directive: scala.Predef.String, suffix: scala.Predef.String): A = {
      if (Play.current.mode == Mode.Dev) {
        super.resource(context, directive, suffix)
      } else {
        Cached(s"$context-$directive-$suffix") { super.resource(context, directive, suffix) }
      }
    }

    override def editorGenerator: SourceGenerator = new EditorGenerator

    override def playerGenerator: SourceGenerator = new PlayerGenerator
  }

  override def playerLauncherActions: PlayerLauncherActions =
    new PlayerLauncherActions(mainSecureSocialService, UserServiceWired) {

      def encryptionEnabled(r: Request[AnyContent]): Boolean = {
        val acceptsFlag = Play.current.mode == Mode.Dev || rootConfig.getBoolean("DEV_TOOLS_ENABLED").getOrElse(false)

        val enabled = if (acceptsFlag) {
          val disable = r.getQueryString("skipDecryption").map(v => true).getOrElse(false)
          !disable
        } else true
        enabled
      }

      def decrypt(request: Request[AnyContent], orgId: ObjectId, contents: String): Option[String] = for {
        encrypter <- Some(if (encryptionEnabled(request)) AESCrypto else NullCrypto)
        orgEncrypter <- Some(new OrgEncrypter(orgId, encrypter))
        out <- orgEncrypter.decrypt(contents)
      } yield out

      def toOrgId(apiClientId: String): Option[ObjectId] = {
        logger.debug(s"[toOrgId] find org for apiClient: $apiClientId")
        val client = ApiClient.findByKey(apiClientId)

        if (client.isEmpty) {
          logger.warn(s"[toOrgId] can't find org for $apiClientId")
        }
        client.map(_.orgId)
      }
    }

  lazy val playerActions = new PlayerActions {

    override def sessionService: MongoService = mainSessionService

    override def itemService: ItemService = ItemServiceWired

    override def transformItem = ItemTransformer.transformToV2Json

    override def auth: AuthenticatedSessionActions = authenticatedSessionActions
  }

  lazy val editorActions = new EditorActions {

    override def itemService: ItemService = ItemServiceWired

    override def transform: (Item) => JsValue = ItemTransformer.transformToV2Json

    override def auth: AuthEditorActions = new AuthEditorActionsCheckPermissions(
      mainSecureSocialService,
      UserServiceWired,
      mainSessionService,
      ItemServiceWired,
      Organization) {

      val permissionGranter = new SimpleWildcardChecker()

      override def hasPermissions(itemId: String, sessionId: Option[String], mode: Mode, options: PlayerOptions): Validation[String, Boolean] = permissionGranter.allow(itemId, sessionId, mode, options) match {
        case Left(error) => Failure(error)
        case Right(allowed) => Success(true)
      }
    }
  }

  lazy val itemActions = new ItemActions {

    override def itemService: ItemService = ItemServiceWired

    override def transform: (Item) => JsValue = ItemTransformer.transformToV2Json

    override def orgService: OrganizationService = Organization

    override def userService: UserService = UserServiceWired

    override def secureSocialService: SecureSocialService = mainSecureSocialService
  }

  lazy val sessionActions = new SessionActions {

    def sessionService: MongoService = mainSessionService

    def itemService: ItemService = ItemServiceWired

    def transformItem: (Item) => JsValue = ItemTransformer.transformToV2Json

    def auth: AuthenticatedSessionActions = authenticatedSessionActions

    def itemPreProcessor: PlayerItemPreProcessor = new RhinoPreProcessor(rootUiComponents, rootLibs)
  }

}
