package org.corespring.v2player.integration

import scala.Some
import scala.concurrent.ExecutionContext

import com.mongodb.casbah.MongoDB
import org.corespring.amazon.s3.{ConcreteS3Service, S3Service}
import org.corespring.common.config.AppConfig
import org.corespring.container.client.component._
import org.corespring.container.client.controllers.{Assets, ComponentSets, DataQuery => ContainerDataQuery}
import org.corespring.container.components.model.Component
import org.corespring.dev.tools.DevTools
import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.core.controllers.auth.SecureSocialService
import org.corespring.platform.core.models.{Organization, Subject}
import org.corespring.platform.core.models.item.{FieldValue, Item}
import org.corespring.platform.core.models.item.resource.StoredFile
import org.corespring.platform.core.services.{QueryService, SubjectQueryService, UserService, UserServiceWired}
import org.corespring.platform.core.services.item.{ItemService, ItemServiceWired}
import org.corespring.platform.core.services.organization.OrganizationService
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2player.integration.actionBuilders._
import org.corespring.v2player.integration.actionBuilders.access.PlayerOptions
import org.corespring.v2player.integration.actionBuilders.access.Mode.Mode
import org.corespring.v2player.integration.actionBuilders.permissions.SimpleWildcardChecker
import org.corespring.v2player.integration.controllers.{DataQuery, DefaultPlayerLauncherActions}
import org.corespring.v2player.integration.controllers.catalog.{AuthCatalogActions, CatalogActions}
import org.corespring.v2player.integration.controllers.editor._
import org.corespring.v2player.integration.controllers.player.{PlayerActions, SessionActions}
import org.corespring.v2player.integration.transformers.ItemTransformer
import play.api.{Configuration, Logger, Mode, Play}
import play.api.cache.Cached
import play.api.libs.json.JsValue
import play.api.mvc._
import scalaz.{Failure, Success, Validation}
import securesocial.core.{Identity, SecureSocial}
import org.corespring.container.components.model.dependencies.DependencyResolver

class V2PlayerIntegration(comps: => Seq[Component],
  val configuration: Configuration,
  db: MongoDB)
  extends org.corespring.container.client.integration.DefaultIntegration {

  lazy val logger = Logger("v2player.integration")

  override def components: Seq[Component] = comps

  lazy val mainSecureSocialService = new SecureSocialService {

    override def currentUser(request: RequestHeader): Option[Identity] = SecureSocial.currentUser(request)
  }

  def itemService: ItemService = ItemServiceWired

  lazy val mainSessionService: MongoService = new MongoService(db("v2.itemSessions"))

  private lazy val authForItem = new AuthItemCheckPermissions(
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

  lazy val assets = new Assets {

    private lazy val key = AppConfig.amazonKey
    private lazy val secret = AppConfig.amazonSecret
    private lazy val bucket = AppConfig.assetsBucket

    lazy val playS3 = new ConcreteS3Service(key, secret)

    import scalaz.Scalaz._
    import scalaz._

    def loadAsset(itemId: String, file: String)(request: Request[AnyContent]): SimpleResult = {

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

    def getItemId(sessionId: String): Option[String] = mainSessionService.load(sessionId).map {
      s => (s \ "itemId").as[String]
    }

    override def actions: AssetActions = new AssetActions {
      override def authForItem: AuthenticatedItem = V2PlayerIntegration.this.authForItem

      override def itemService: ItemService = ItemServiceWired

      override def bucket: String = AppConfig.assetsBucket

      override def s3: S3Service = playS3
    }
  }

  lazy val componentUrls: ComponentSets = new ComponentSets {

    override def dependencyResolver: DependencyResolver = new DependencyResolver {
      override def components: Seq[Component] = comps
    }

    override def allComponents: Seq[Component] = comps

    override def resource[A >: play.api.mvc.EssentialAction](context: scala.Predef.String, directive: scala.Predef.String, suffix: scala.Predef.String): A = {
      if (Play.current.mode == Mode.Dev) {
        super.resource(context, directive, suffix)
      } else {
        implicit val current = play.api.Play.current
        Cached(s"$context-$directive-$suffix") {
          super.resource(context, directive, suffix)
        }
      }
    }

    override def editorGenerator: SourceGenerator = new EditorGenerator

    override def playerGenerator: SourceGenerator = new PlayerGenerator

    override def catalogGenerator: SourceGenerator = new CatalogGenerator
  }

  override val playerLauncherActions: PlayerLauncherActions =
    new DefaultPlayerLauncherActions(mainSecureSocialService, UserServiceWired, configuration)

  override val playerActions = new PlayerActions {
    override def auth: AuthenticatedSessionActions = authenticatedSessionActions

    override def transformItem: (Item) => JsValue = ItemTransformer.transformToV2Json

    override def itemService: ItemService = ItemServiceWired

    override def sessionService: MongoService = mainSessionService
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

  lazy val catalogActions = new CatalogActions {

    override def itemService: ItemService = ItemServiceWired

    override def transform: (Item) => JsValue = ItemTransformer.transformToV2Json

    override def auth: AuthCatalogActions = new AuthCatalogActionsCheckPermissions(
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

  lazy val itemHooks = new ItemHooks {

    override def itemService: ItemService = ItemServiceWired

    override def transform: (Item) => JsValue = ItemTransformer.transformToV2Json

    override def orgService: OrganizationService = Organization

    override def userService: UserService = UserServiceWired

    override def secureSocialService: SecureSocialService = mainSecureSocialService

    override implicit def executionContext: ExecutionContext = ExecutionContext.Implicits.global
  }

  lazy val sessionActions = new SessionActions {

    def sessionService: MongoService = mainSessionService

    def itemService: ItemService = ItemServiceWired

    def transformItem: (Item) => JsValue = ItemTransformer.transformToV2Json

    def auth: AuthenticatedSessionActions = authenticatedSessionActions
  }

  override def dataQuery: ContainerDataQuery = new DataQuery() {
    override def subjectQueryService: QueryService[Subject] = SubjectQueryService

    override def fieldValues: FieldValue = FieldValue.findAll().toSeq.head
  }
}
