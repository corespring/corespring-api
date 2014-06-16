package org.corespring.v2player.integration

import com.mongodb.casbah.MongoDB
import org.bson.types.ObjectId
import org.corespring.amazon.s3.{ ConcreteS3Service, S3Service }
import org.corespring.common.config.AppConfig
import org.corespring.common.encryption.{ AESCrypto, NullCrypto }
import org.corespring.container.client.component._
import org.corespring.container.client.controllers.{ Assets, ComponentSets, DataQuery => ContainerDataQuery }
import org.corespring.container.components.model.Component
import org.corespring.container.components.model.dependencies.DependencyResolver
import org.corespring.dev.tools.DevTools
import org.corespring.mongo.json.services.MongoService
import org.corespring.platform.core.controllers.auth.SecureSocialService
import org.corespring.platform.core.encryption.OrgEncrypter
import org.corespring.platform.core.models.auth.ApiClient
import org.corespring.platform.core.models.item.resource.StoredFile
import org.corespring.platform.core.models.item.{ FieldValue, Item }
import org.corespring.platform.core.models.{ Organization, Subject }
import org.corespring.platform.core.services.item.{ ItemService, ItemServiceWired }
import org.corespring.platform.core.services.organization.OrganizationService
import org.corespring.platform.core.services.{ QueryService, SubjectQueryService, UserService, UserServiceWired }
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.v2.auth.{ OrgTransformer, WithOrgTransformerSequence, WithServiceOrgTransformer }
import org.corespring.v2player.integration.actionBuilders._
import org.corespring.v2player.integration.actionBuilders.access.Mode.Mode
import org.corespring.v2player.integration.actionBuilders.access.PlayerOptions
import org.corespring.v2player.integration.actionBuilders.permissions.SimpleWildcardChecker
import org.corespring.v2player.integration.controllers.catalog.{ AuthCatalogActions, CatalogActions }
import org.corespring.v2player.integration.controllers.editor._
import org.corespring.v2player.integration.controllers.player.{ PlayerActions, SessionActions }
import org.corespring.v2player.integration.controllers.{ DataQuery, DefaultPlayerLauncherActions }
import org.corespring.v2player.integration.transformers.ItemTransformer
import play.api.cache.Cached
import play.api.libs.json.JsValue
import play.api.mvc._
import play.api.{ Configuration, Logger, Mode, Play }
import securesocial.core.{ Identity, SecureSocial }

import scala.concurrent.ExecutionContext
import scalaz.{ Failure, Success, Validation }

class V2PlayerIntegration(comps: => Seq[Component],
  val configuration: Configuration,
  db: MongoDB)
  extends org.corespring.container.client.integration.DefaultIntegration {

  lazy val logger = Logger("v2player.integration")

  override def components: Seq[Component] = comps

  lazy val secureSocialService = new SecureSocialService {

    override def currentUser(request: RequestHeader): Option[Identity] = SecureSocial.currentUser(request)
  }

  def encryptionEnabled(r: RequestHeader): Boolean = {
    val acceptsFlag = Play.current.mode == Mode.Dev || configuration.getBoolean("DEV_TOOLS_ENABLED").getOrElse(false)

    val enabled = if (acceptsFlag) {
      val disable = r.getQueryString("skipDecryption").map(v => true).getOrElse(false)
      !disable
    } else true
    enabled
  }

  def decrypt(request: RequestHeader, orgId: ObjectId, contents: String): Option[String] = for {
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

  def transformer: OrgTransformer[(ObjectId, PlayerOptions)] = new WithOrgTransformerSequence[(ObjectId, PlayerOptions)] {
    override def transformers: Seq[WithServiceOrgTransformer[(ObjectId, PlayerOptions)]] = Seq.empty
  }

  lazy val sessionService: MongoService = new MongoService(db("v2.itemSessions"))

  def itemService: ItemService = ItemServiceWired

  lazy val baseAuthCheck = new BaseAuthCheck(
    V2PlayerIntegration.this.secureSocialService,
    UserServiceWired,
    V2PlayerIntegration.this.sessionService,
    ItemServiceWired,
    Organization) {

    val permissionGranter = new SimpleWildcardChecker()

    override def hasPermissions(itemId: String, sessionId: Option[String], mode: Mode, options: PlayerOptions): Validation[String, Boolean] = permissionGranter.allow(itemId, sessionId, mode, options) match {
      case Left(error) => Failure(error)
      case Right(allowed) => Success(true)
    }

    override def decrypt(request: RequestHeader, orgId: ObjectId, encrypted: String): Option[String] = V2PlayerIntegration.this.decrypt(request, orgId, encrypted)

    override def transformer: OrgTransformer[(ObjectId, PlayerOptions)] = V2PlayerIntegration.this.transformer

    override def toOrgId(apiClientId: String): Option[ObjectId] = V2PlayerIntegration.this.toOrgId(apiClientId)
  }

  private lazy val authForItem = new AuthItemCheckPermissions(sessionService, baseAuthCheck) {}

  private lazy val authActions = new AuthSessionActionsCheckPermissions(sessionService, baseAuthCheck) {}

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

    def getItemId(sessionId: String): Option[String] = sessionService.load(sessionId).map {
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
    new DefaultPlayerLauncherActions(secureSocialService, UserServiceWired, configuration) {

      override def decrypt(request: RequestHeader, orgId: ObjectId, encrypted: String): Option[String] = V2PlayerIntegration.this.decrypt(request, orgId, encrypted)

      override def toOrgId(apiClientId: String): Option[ObjectId] = V2PlayerIntegration.this.toOrgId(apiClientId)

      override def transformer: OrgTransformer[(ObjectId, PlayerOptions)] = V2PlayerIntegration.this.transformer

    }

  override val playerActions = new PlayerActions {
    override def auth: AuthenticatedSessionActions = authenticatedSessionActions

    override def transformItem: (Item) => JsValue = ItemTransformer.transformToV2Json

    override def itemService: ItemService = ItemServiceWired

    override def sessionService: MongoService = V2PlayerIntegration.this.sessionService
  }

  lazy val editorActions = new EditorActions {

    override def itemService: ItemService = ItemServiceWired

    override def transform: (Item) => JsValue = ItemTransformer.transformToV2Json

    override def auth: AuthEditorActions = new AuthEditorActionsCheckPermissions(baseAuthCheck) {}
  }

  lazy val catalogActions = new CatalogActions {

    override def itemService: ItemService = ItemServiceWired

    override def transform: (Item) => JsValue = ItemTransformer.transformToV2Json

    override def auth: AuthCatalogActions = new AuthCatalogActionsCheckPermissions(baseAuthCheck) {}
  }

  lazy val itemHooks = new ItemHooks {

    override def itemService: ItemService = ItemServiceWired

    override def transform: (Item) => JsValue = ItemTransformer.transformToV2Json

    override def orgService: OrganizationService = Organization

    override def userService: UserService = UserServiceWired

    override def secureSocialService: SecureSocialService = V2PlayerIntegration.this.secureSocialService

    override implicit def executionContext: ExecutionContext = ExecutionContext.Implicits.global

    override def decrypt(request: RequestHeader, orgId: ObjectId, encrypted: String): Option[String] = V2PlayerIntegration.this.decrypt(request, orgId, encrypted)

    override def transformer: OrgTransformer[(ObjectId, PlayerOptions)] = V2PlayerIntegration.this.transformer

    override def toOrgId(apiClientId: String): Option[ObjectId] = V2PlayerIntegration.this.toOrgId(apiClientId)
  }

  lazy val sessionActions = new SessionActions {

    def sessionService: MongoService = V2PlayerIntegration.this.sessionService

    def itemService: ItemService = ItemServiceWired

    def transformItem: (Item) => JsValue = ItemTransformer.transformToV2Json

    def auth: AuthenticatedSessionActions = authenticatedSessionActions
  }

  override def dataQuery: ContainerDataQuery = new DataQuery() {
    override def subjectQueryService: QueryService[Subject] = SubjectQueryService

    override def fieldValues: FieldValue = FieldValue.findAll().toSeq.head
  }
}
