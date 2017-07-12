package bootstrap

import java.net.URL

import bootstrap.Actors.UpdateItem
import com.amazonaws.auth.AWSCredentials
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.s3.transfer.TransferManager
import com.amazonaws.services.s3.{ AmazonS3, S3ClientOptions }
import com.mongodb.casbah.MongoDB
import com.novus.salat.Context
import developer.{ DeveloperConfig, DeveloperModule }
import filters.{ BlockingFutureQueuer, CacheFilter, FutureQueuer }
import org.bson.types.ObjectId
import org.corespring.amazon.s3.{ ConcreteS3Service, S3Service }
import org.corespring.api.tracking.{ ApiTracking, ApiTrackingLogger, NullTracking }
import org.corespring.api.v1.{ V1ApiExecutionContext, V1ApiModule }
import org.corespring.assets.{ EncodedKeyS3Client, ItemAssetKeys }
import org.corespring.common.config.{ CdnConfig, ItemAssetResolverConfig }
import org.corespring.container.client.component.{ ComponentSetExecutionContext, ComponentsConfig }
import org.corespring.container.client.controllers.resources.SessionExecutionContext
import org.corespring.container.client.integration.{ ContainerConfig, ContainerExecutionContext }
import org.corespring.container.client.{ NewRelicRumConfig, V2PlayerConfig, VersionInfo }
import org.corespring.container.components.loader.{ ComponentLoader, FileComponentLoader }
import org.corespring.container.components.model.Component
import org.corespring.conversion.qti.transformers.{ ItemTransformer, ItemTransformerConfig, PlayerJsonToItem }
import org.corespring.csApi.buildInfo.BuildInfo
import org.corespring.drafts.item.DraftAssetKeys
import org.corespring.drafts.item.models.{ DraftId, OrgAndUser, SimpleOrg, SimpleUser }
import org.corespring.drafts.item.services.ItemDraftConfig
import org.corespring.encryption.EncryptionModule
import org.corespring.importing.validation.ItemSchema
import org.corespring.importing.{ ImportingExecutionContext, ItemImportModule }
import org.corespring.itemSearch.{ ElasticSearchConfig, ElasticSearchExecutionContext, ItemSearchModule }
import org.corespring.legacy.ServiceLookup
import org.corespring.models.appConfig._
import org.corespring.models.auth.ApiClient
import org.corespring.models.item.{ ComponentType, FieldValue, Item }
import org.corespring.models.json.JsonFormatting
import org.corespring.models.{ Standard, Subject }
import org.corespring.platform.core.LegacyModule
import org.corespring.platform.core.services.item.SupportingMaterialsAssets
import org.corespring.platform.data.VersioningDao
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.salat.ServicesContext
import org.corespring.services.salat.bootstrap._
import org.corespring.v2.actions.{ DefaultV2Actions, V2ActionExecutionContext }
import org.corespring.v2.api._
import org.corespring.v2.api.drafts.item.ItemDraftsConfig
import org.corespring.v2.api.services.{ BasicScoreService, OrgScoringExecutionContext, ScoreService, ScoreServiceExecutionContext }
import org.corespring.v2.auth.identifiers.{ PlayerTokenConfig, UserSessionOrgIdentity }
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.auth.{ AccessSettingsCheckConfig, V2AuthModule }
import org.corespring.v2.errors.V2Error
import org.corespring.v2.player._
import org.corespring.v2.player.cdn._
import org.corespring.v2.player.hooks.StandardsTree
import org.corespring.v2.player.services.item.{ DraftSupportingMaterialsService, ItemSupportingMaterialsService, MongoDraftSupportingMaterialsService, MongoItemSupportingMaterialsService }
import org.corespring.v2.sessiondb._
import org.corespring.web.common.controllers.deployment.{ AssetsLoader, BranchAndHash }
import org.corespring.web.user.SecureSocial
import org.joda.time.DateTime
import play.api.Mode.{ Mode => PlayMode }
import play.api.libs.json.{ JsArray, Json }
import play.api.mvc._
import play.api.{ Configuration, Logger, Mode }
import play.libs.Akka
import se.radley.plugin.salat.SalatPlugin
import v2Player.{ Routes => V2PlayerRoutes }
import web.models.WebExecutionContext
import web.{ PublicSiteConfig, WebModule, WebModuleConfig, WebV2Actions }

import scala.concurrent.{ ExecutionContext, Future }
import scalaz.Validation

object Main {
  def apply(app: play.api.Application): Main = {

    def salatDb(dbName: String) = app.plugins
      .find(p => classOf[SalatPlugin].isAssignableFrom(p.getClass))
      .map(_.asInstanceOf[SalatPlugin])
      .map(_.db(dbName))
      .getOrElse {
        throw new RuntimeException("Can't find SalatPlugin so can't load the db")
      }

    lazy val db: MongoDB = salatDb("default")
    lazy val archiveDb: MongoDB = salatDb("archive")

    new Main(db, archiveDb, app.configuration, app.mode, app.classloader, app.resource)
  }
}

class Main(
  val db: MongoDB,
  archive: MongoDB,
  //TODO: rm Configuration (needed for [[HasConfig]]) and use appConfig + containerConfig instead.
  val configuration: Configuration,
  val inputMode: PlayMode,
  classLoader: ClassLoader,
  resourceAsURL: String => Option[URL])
  extends SalatServices
  with DeveloperModule
  with EncryptionModule
  with ItemImportModule
  with ItemSearchModule
  with LegacyModule
  with SessionDbModule
  with V1ApiModule
  with V2ApiModule
  with V2AuthModule
  with V2PlayerModule
  with WebModule {

  override val loadResource: String => Option[URL] = resourceAsURL(_)

  import com.softwaremill.macwire.MacwireMacros._

  private lazy val logger = Logger(this.getClass)

  override def playMode: PlayMode = {
    configuration.getString("APP_MODE_OVERRIDE").map(Mode.withName).getOrElse(inputMode)
  }

  lazy val appConfig = AppConfig(configuration)

  lazy val archiveDb = appConfig.sessionServiceArchiveEnabled match {
    case true => Some(archive)
    case _ => None
  }

  override lazy val containerConfig: ContainerConfig = {
    val newRelicRumConfig = NewRelicRumConfig.fromConfig(configuration.getConfig("newrelic.rum.applications.player").getOrElse(Configuration.empty))

    val launchTimeout: Int = configuration.getInt("container.launchTimeout").getOrElse(0)
    val playerConfig = V2PlayerConfig(
      rootUrl = configuration.getString("container.rootUrl"),
      newRelicRumConfig = newRelicRumConfig,
      launchTimeout)

    ContainerConfig(
      mode = playMode,
      showNonReleasedComponents = configuration.getBoolean("container.components.showNonReleasedComponents").getOrElse(playMode == Mode.Dev),
      editorDebounceInMillis = configuration.getLong("container.editor.autosave.debounceInMillis").getOrElse(5000),
      components = ComponentsConfig.fromConfig(playMode, resolveDomain(V2PlayerRoutes.prefix + "/images"), configuration.getConfig("container.components").getOrElse(Configuration.empty)),
      player = playerConfig,
      uploadAudioMaxSizeKb = configuration.getLong("container.editor.upload.audio.maxSizeKb").getOrElse(16 * 1024 - 1),
      uploadImageMaxSizeKb = configuration.getLong("container.editor.upload.image.maxSizeKb").getOrElse(500))
  }

  logger.info(s"containerConfig: $containerConfig")

  logger.info(s"versionInfo: $versionInfo")

  override lazy val accessSettingsCheckConfig: AccessSettingsCheckConfig = AccessSettingsCheckConfig(appConfig.allowAllSessions)

  override lazy val developerConfig: DeveloperConfig = DeveloperConfig(appConfig.demoOrgId)

  override lazy val defaultOrgs: DefaultOrgs = DefaultOrgs(appConfig.v2playerOrgIds, appConfig.rootOrgId)

  override lazy val publicSiteConfig: PublicSiteConfig = PublicSiteConfig(appConfig.publicSite)

  private def ecLookup(id: String) = {
    def hasEnabledAkkaConfiguration(id: String) = {
      (for {
        configDoesExist <- configuration.getObject(id)
        configIsEnabled <- configuration.getBoolean(id + ".enabled")
      } yield configIsEnabled).getOrElse(false)
    }
    if (hasEnabledAkkaConfiguration(id)) {
      logger.info(s"Using specific execution context for $id")
      Akka.system.dispatchers.lookup(id)
    } else {
      logger.info(s"Using global execution context for $id")
      ExecutionContext.global
    }
  }

  override lazy val componentSetExecutionContext = ComponentSetExecutionContext(ecLookup("akka.component-set-heavy"))
  override lazy val containerContext: ContainerExecutionContext = ContainerExecutionContext(ExecutionContext.global)
  override lazy val elasticSearchExecutionContext = ElasticSearchExecutionContext(ecLookup("akka.elastic-search"))
  override lazy val importingExecutionContext: ImportingExecutionContext = ImportingExecutionContext(ecLookup("akka.import"))
  override lazy val itemApiExecutionContext: ItemApiExecutionContext = ItemApiExecutionContext(ExecutionContext.global)
  override lazy val itemSessionApiExecutionContext: ItemSessionApiExecutionContext = ItemSessionApiExecutionContext(ExecutionContext.global)
  override lazy val salatServicesExecutionContext = SalatServicesExecutionContext(ecLookup("akka.salat-services"))
  override lazy val scoringApiExecutionContext: ScoringApiExecutionContext = ScoringApiExecutionContext(ecLookup("akka.scoring-default"), ecLookup("akka.scoring-heavy"))
  override lazy val sessionExecutionContext = SessionExecutionContext(ecLookup("akka.session-default"), ecLookup("akka.session-heavy"))
  override lazy val v1ApiExecutionContext = V1ApiExecutionContext(ecLookup("akka.v1-api"))
  override lazy val v2ApiExecutionContext = V2ApiExecutionContext(ecLookup("akka.v2-api"))
  override lazy val v2PlayerExecutionContext = V2PlayerExecutionContext(ecLookup("akka.v2-player"))
  override lazy val webExecutionContext: WebExecutionContext = WebExecutionContext(ecLookup("akka.web"))
  lazy val v2ActionContext: V2ActionExecutionContext = V2ActionExecutionContext(ecLookup("akka.v2-actions"))
  override lazy val sessionServiceExecutionContext: SessionServiceExecutionContext = SessionServiceExecutionContext(sessionExecutionContext.heavyLoad)
  override lazy val orgScoringExecutionContext: OrgScoringExecutionContext = OrgScoringExecutionContext(scoringApiExecutionContext.contextForScoring)
  override lazy val scoreServiceExecutionContext = ScoreServiceExecutionContext(scoringApiExecutionContext.contextForScoring)

  private def mainAppVersion(): String = {
    val commit = BuildInfo.commitHash
    val versionOverride = appConfig.appVersionOverride
    val result = commit + versionOverride
    logger.trace(s"AppVersion $result hash ${commit} override ${versionOverride}")
    result
  }

  lazy val componentSetFilter = new CacheFilter {
    override implicit def ec: ExecutionContext = componentSetExecutionContext.heavyLoad

    override lazy val bucket: String = Main.this.bucket.bucket

    override def appVersion: String = {
      mainAppVersion()
    }

    override def s3: AmazonS3 = Main.this.s3

    override def intercept(path: String) = path.contains("component-sets") && !path.contains(".less")

    override val gzipEnabled = containerConfig.components.gzip

    override lazy val futureQueue: FutureQueuer = new BlockingFutureQueuer()
  }

  override lazy val externalModelLaunchConfig: ExternalModelLaunchConfig = ExternalModelLaunchConfig(
    org.corespring.container.client.controllers.launcher.player.routes.PlayerLauncher.playerJs().url)

  logger.debug(s"bootstrapping... ${mainAppVersion()}")

  lazy val controllers: Seq[Controller] = {
    v2PlayerControllers ++
      v2ApiControllers ++
      v1ApiControllers ++
      webControllers ++
      itemImportControllers ++
      developerControllers :+
      itemDraftsController
  }

  private lazy val cdnConfig = CdnConfig(
    configuration.getString("container.cdn.domain"),
    configuration.getBoolean("container.cdn.add-version-as-query-param").getOrElse(true))

  lazy val cdnResolver = new CdnResolver(
    cdnConfig.domain,
    if (cdnConfig.addVersionAsQueryParam) Some(mainAppVersion) else None)

  override def resolveDomain(path: String): String = cdnResolver.resolveDomain(path)

  override lazy val itemAssetResolver: ItemAssetResolver = {
    val config = ItemAssetResolverConfig(configuration, playMode)
    if (config.enabled) {
      val version = if (config.addVersionAsQueryParam) Some(mainAppVersion) else None
      val cdnResolver: CdnResolver = if (config.signUrls) {
        val keyPairId = config.keyPairId.getOrElse(throw new RuntimeException("ItemAssetResolver: keyPairId is not set"))
        val privateKey = config.privateKey.getOrElse(throw new RuntimeException("ItemAssetResolver: privateKey is not set"))
        val urlSigner = new CdnUrlSigner(keyPairId, privateKey)
        new SignedUrlCdnResolver(config.domain, version, urlSigner, config.urlExpiresAfterMinutes, config.httpProtocolForSignedUrls)
      } else {
        new CdnResolver(config.domain, version)
      }
      new CdnItemAssetResolver(cdnResolver)
    } else {
      new DisabledItemAssetResolver
    }
  }

  override lazy val itemDraftsConfig = ItemDraftsConfig(appConfig.loadExpiredItemDrafts)

  override lazy val elasticSearchConfig = ElasticSearchConfig(
    appConfig.elasticSearchUrl,
    appConfig.mongoUri,
    containerConfig.components.componentsPath)

  lazy val transformerItemService = new TransformerItemService(
    itemService,
    db(CollectionNames.versionedItem),
    db(CollectionNames.item))(context)

  lazy val itemTransformerConfig = ItemTransformerConfig(
    configuration.getBoolean("v2.itemTransformer.checkModelIsUpToDate").getOrElse(false))

  override lazy val sessionDbConfig: SessionDbConfig = {
    val envName = if (appConfig.dynamoDbActivate) Some(appConfig.envName) else None
    new SessionDbConfig(appConfig.sessionService, appConfig.sessionServiceUrl, appConfig.sessionServiceAuthToken,
      envName, appConfig.dynamoDbUseLocal, appConfig.dynamoDbLocalInit)
  }

  override lazy val awsCredentials: AWSCredentials = appConfig.s3Config.credentials
  override lazy val dbClient: AmazonDynamoDBClient = new AmazonDynamoDBClient(awsCredentials)

  override lazy val itemTransformer: ItemTransformer = wire[AllItemVersionTransformer]

  private lazy val actor = Actors.itemTransformerActor(itemTransformer)

  override lazy val sessionCreatedCallback: VersionedId[ObjectId] => Unit = {
    (itemId) => actor ! UpdateItem(itemId)
  }

  override lazy val componentTypes: Seq[ComponentType] = {

    def toComponentType(c: Component) = {
      val label = (c.packageInfo \ "title").asOpt[String].getOrElse(c.componentType)
      ComponentType(c.componentType, label)
    }

    componentLoader.all
      .filterNot(_.componentType == "corespring-feedback-block")
      .map(toComponentType) :+ ComponentType("multiple-interactions", "Multiple Interactions")
  }

  //Used for wiring RequestIdentifiers
  private lazy val secureSocial: SecureSocial = new SecureSocial {}

  override lazy val userSessionOrgIdentity: UserSessionOrgIdentity = requestIdentifiers.userSession

  private lazy val playerTokenConfig: PlayerTokenConfig = {
    PlayerTokenConfig(canSkipDecryption = playMode == Mode.Dev || playMode == Mode.Test)
  }

  private lazy val requestIdentifiers: RequestIdentifiers = wire[RequestIdentifiers]

  override lazy val getOrgAndOptsFn: (RequestHeader) => Validation[V2Error, OrgAndOpts] = requestIdentifiers.allIdentifiers.apply

  override def getOrgOptsAndApiClientFn: (RequestHeader) => Validation[V2Error, (OrgAndOpts, ApiClient)] = requestIdentifiers.accessTokenToOrgAndApiClient

  /**
   * Note: macwire > 1.0 has a tagging option so that you can tag instances of the same type
   * in a scope so it knows which property to inject (eg if we have multiple strings in scope)
   * However this won't be available until we move to scala 2.11 and will probably involve a bump of play too.
   * In the interim we create simple types to wrap the strings
   */
  override lazy val bucket = Bucket(appConfig.s3Config.bucket)

  override lazy val archiveConfig = ArchiveConfig(appConfig.archiveContentCollectionId, appConfig.archiveOrgId)

  override lazy val accessTokenConfig = AccessTokenConfig()

  override lazy val allowExpiredTokens = AllowExpiredTokens(appConfig.allowExpiredTokens)

  override lazy val transferManager: TransferManager = new TransferManager(s3)

  override lazy val s3: AmazonS3 = {

    logger.info("val=s3 - creating new CorespringS3Client")
    val client = new EncodedKeyS3Client(awsCredentials)

    appConfig.s3Config.endpoint.foreach { e =>
      val options = new S3ClientOptions()
      client.setEndpoint(e)
      options.withPathStyleAccess(true)
      client.setS3ClientOptions(options)
    }

    client
  }

  override lazy val context: Context = new ServicesContext(classLoader)

  override lazy val identifyUser: RequestHeader => Option[OrgAndUser] = (rh) => {

    def orgAndOptsToOrgAndUser(o: OrgAndOpts): OrgAndUser = OrgAndUser(
      SimpleOrg.fromOrganization(o.org),
      o.user.map(SimpleUser.fromUser))

    getOrgAndOptsFn.apply(rh).map(o => orgAndOptsToOrgAndUser(o)).toOption
  }

  override lazy val jsonFormatting: JsonFormatting = new JsonFormatting {
    override lazy val findStandardByDotNotation: (String) => Option[Standard] = standardService.findOneByDotNotation(_)

    private lazy val fieldValueLoadedOnce = fieldValueService.get.get

    override def fieldValue: FieldValue = if (playMode == Mode.Prod) {
      fieldValueLoadedOnce
    } else {
      fieldValueService.get.get
    }

    override lazy val findSubjectById: (ObjectId) => Option[Subject] = subjectService.findOneById(_)

    override lazy val rootOrgId: ObjectId = appConfig.rootOrgId
  }

  override lazy val itemDao: VersioningDao[Item, VersionedId[ObjectId]] = {
    logger.debug(s"initializing itemDao to be ItemIndexingDao")
    new ItemIndexingDao(salatItemDao, itemIndexService, ExecutionContext.global)
  }

  def initServiceLookup() = {
    logger.info("Initialising legacy services using `ServiceLookup`...")
    ServiceLookup.demoOrgId = appConfig.demoOrgId
    ServiceLookup.apiClientService = apiClientService
    ServiceLookup.contentCollectionService = contentCollectionService
    ServiceLookup.itemService = itemService
    ServiceLookup.jsonFormatting = jsonFormatting
    ServiceLookup.orgService = orgService
    ServiceLookup.registrationTokenService = registrationTokenService
    ServiceLookup.userService = userService
    ServiceLookup.fieldValueService = fieldValueService
    ServiceLookup.standardService = standardService
    ServiceLookup.subjectService = subjectService
  }

  override def s3Service: S3Service = wire[ConcreteS3Service]

  lazy val componentLoader: ComponentLoader = {
    val path = containerConfig.components.componentsPath
    val loader = new FileComponentLoader(Seq(path))
    loader.reload
    loader
  }

  override lazy val standardTree: StandardsTree = {
    val json: JsArray = {
      resourceLoader.loadPath("public/web/standards_tree.json").map { s =>
        Json.parse(s).as[JsArray]
      }.getOrElse(throw new RuntimeException("Can't find web/standards_tree.json"))
    }
    StandardsTree(json)
  }

  lazy val itemAssetKeys = ItemAssetKeys
  lazy val draftAssetKeys = DraftAssetKeys

  lazy val itemSupportingMaterialAssets: SupportingMaterialsAssets[VersionedId[ObjectId]] = wire[SupportingMaterialsAssets[VersionedId[ObjectId]]]
  lazy val draftSupportingMaterialAssets: SupportingMaterialsAssets[DraftId] = wire[SupportingMaterialsAssets[DraftId]]

  override lazy val itemSupportingMaterialsService: ItemSupportingMaterialsService = new MongoItemSupportingMaterialsService(
    db(CollectionNames.item),
    bucket,
    itemSupportingMaterialAssets)(context)

  override lazy val draftSupportingMaterialsService: DraftSupportingMaterialsService = new MongoDraftSupportingMaterialsService(
    db(ItemDraftConfig.CollectionNames.itemDrafts),
    bucket,
    draftSupportingMaterialAssets)(context)

  //TODO: RF: Plugin in session service
  override def mostRecentDateModifiedForSessions: (Seq[ObjectId]) => Option[DateTime] = _ => None

  lazy val apiTracking: ApiTracking = {

    lazy val logRequests = {
      val out = configuration.getBoolean("api.log-requests").getOrElse(playMode == Mode.Dev)
      logger.info(s"Log api requests? $out")
      out
    }

    if (logRequests) {
      wire[ApiTrackingLogger]
    } else {
      NullTracking
    }
  }

  override lazy val itemSchema: ItemSchema = {
    val file = "schema/item-schema.json"
    val schemaString = resourceLoader
      .loadPath("schema/item-schema.json")
      .getOrElse(throw new IllegalArgumentException(s"File $file not found"))
    val schema = ItemSchema(schemaString)
    schema
  }

  override lazy val playerJsonToItem: PlayerJsonToItem = new PlayerJsonToItem(jsonFormatting)

  override lazy val assetsLoader: AssetsLoader = new AssetsLoader(playMode, configuration, s3, BranchAndHash(
    BuildInfo.branch,
    BuildInfo.commitHash))

  initServiceLookup()
  componentLoader.reload

  lazy val futureAuth = (request: RequestHeader) => Future { getOrgAndOptsFn(request) }(v2ApiExecutionContext.context)

  override lazy val versionInfo: VersionInfo = VersionInfo(configuration.getConfig("container").getOrElse(Configuration.empty))

  override def webV2Actions: WebV2Actions = WebV2Actions(new DefaultV2Actions(
    defaultOrgs,
    rh => Future { userSessionOrgIdentity.apply(rh) }(webExecutionContext.context),
    apiClientService,
    v2ActionContext))

  override lazy val v2ApiActions: V2ApiActions = V2ApiActions(new DefaultV2Actions(
    defaultOrgs,

    /**
     * TODO: We have to support userSession here because legacy v1 api endpoints are used in the cms
     * These are routed to v2.
     */
    rh => Future { requestIdentifiers.allIdentifiers(rh) }(v2ApiExecutionContext.context),
    apiClientService,
    v2ActionContext))

  override lazy val webModuleConfig: WebModuleConfig = WebModuleConfig(playMode)

}
