package bootstrap

import bootstrap.Actors.UpdateItem
import com.amazonaws.auth.AWSCredentials
import com.amazonaws.services.s3.transfer.TransferManager
import com.amazonaws.services.s3.{ AmazonS3, AmazonS3Client, S3ClientOptions }
import com.mongodb.casbah.MongoDB
import com.novus.salat.Context
import common.db.Db
import developer.DeveloperModule
import filters.{ BlockingFutureQueuer, CacheFilter, FutureQueuer }
import org.apache.commons.io.IOUtils
import org.bson.types.ObjectId
import org.corespring.amazon.s3.S3Service
import org.corespring.api.tracking.{ ApiTracking, ApiTrackingLogger, NullTracking }
import org.corespring.api.v1.{ V1ApiExecutionContext, V1ApiModule }
import org.corespring.assets.{ CorespringS3ServiceExtended, ItemAssetKeys }
import org.corespring.common.config.{ AppConfig, ContainerConfig }
import org.corespring.container.client.{ComponentSetExecutionContext, ItemAssetResolver}
import org.corespring.container.client.controllers.resources.SessionExecutionContext
import org.corespring.container.client.integration.ContainerExecutionContext
import org.corespring.container.components.loader.{ ComponentLoader, FileComponentLoader }
import org.corespring.container.components.model.Component
import org.corespring.conversion.qti.transformers.{ ItemTransformer, ItemTransformerConfig, PlayerJsonToItem }
import org.corespring.drafts.item.DraftAssetKeys
import org.corespring.drafts.item.models.{ DraftId, OrgAndUser, SimpleOrg, SimpleUser }
import org.corespring.drafts.item.services.ItemDraftConfig
import org.corespring.encryption.EncryptionModule
import org.corespring.importing.validation.ItemSchema
import org.corespring.importing.{ ImportingExecutionContext, ItemImportModule }
import org.corespring.itemSearch.{ ElasticSearchConfig, ElasticSearchExecutionContext, ItemSearchModule }
import org.corespring.legacy.ServiceLookup
import org.corespring.models.appConfig.{ AccessTokenConfig, ArchiveConfig, Bucket }
import org.corespring.models.item.{ ComponentType, FieldValue, Item }
import org.corespring.models.json.JsonFormatting
import org.corespring.models.{ Standard, Subject }
import org.corespring.platform.core.LegacyModule
import org.corespring.platform.core.services.item.SupportingMaterialsAssets
import org.corespring.platform.data.VersioningDao
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.services.salat.ServicesContext
import org.corespring.services.salat.bootstrap._
import org.corespring.v2.api._
import org.corespring.v2.api.services.{ BasicScoreService, ScoreService }
import org.corespring.v2.auth.V2AuthModule
import org.corespring.v2.auth.identifiers.UserSessionOrgIdentity
import org.corespring.v2.auth.models.OrgAndOpts
import org.corespring.v2.errors.V2Error
import org.corespring.v2.player._
import org.corespring.v2.player.hooks.StandardsTree
import org.corespring.v2.player.services.item.{ DraftSupportingMaterialsService, ItemSupportingMaterialsService, MongoDraftSupportingMaterialsService, MongoItemSupportingMaterialsService }
import org.corespring.v2.sessiondb._
import org.corespring.web.common.views.helpers.BuildInfo
import org.corespring.web.user.SecureSocial
import org.joda.time.DateTime
import play.api.Mode.{ Mode => PlayMode }
import play.api.libs.json.{ JsArray, Json }
import play.api.mvc._
import play.api.{ Logger, Mode, Play }
import play.libs.Akka
import web.WebModule
import web.controllers.{ Main, ShowResource }
import web.models.{ ContainerVersion, WebExecutionContext }

import scala.concurrent.ExecutionContext
import scalaz.Validation

object Main
  extends SalatServices
  with EncryptionModule
  with ItemSearchModule
  with V2AuthModule
  with V2ApiModule
  with V1ApiModule
  with V2PlayerModule
  with SessionDbModule
  with LegacyModule
  with DeveloperModule
  with WebModule
  with ItemImportModule {

  import com.softwaremill.macwire.MacwireMacros._
  import play.api.Play.current

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

  override lazy val containerVersion: ContainerVersion = ContainerVersion(versionInfo)

  override lazy val componentSetExecutionContext = ComponentSetExecutionContext(ecLookup("akka.component-set-heavy"))
  override lazy val elasticSearchExecutionContext = ElasticSearchExecutionContext(ecLookup("akka.elastic-search"))
  override lazy val importingExecutionContext: ImportingExecutionContext = ImportingExecutionContext(ecLookup("akka.import"))
  override lazy val salatServicesExecutionContext = SalatServicesExecutionContext(ecLookup("akka.salat-services"))
  override lazy val sessionExecutionContext = SessionExecutionContext(ecLookup("akka.session-default"), ecLookup("akka.session-heavy"))
  override lazy val v1ApiExecutionContext = V1ApiExecutionContext(ecLookup("akka.v1-api"))
  override lazy val v2ApiExecutionContext = V2ApiExecutionContext(ecLookup("akka.v2-api"))
  override lazy val v2PlayerExecutionContext = V2PlayerExecutionContext(ecLookup("akka.v2-player"))
  override def webExecutionContext: WebExecutionContext = WebExecutionContext(ecLookup("akka.web"))

  private def mainAppVersion(): String = {
    val commit = BuildInfo.commitHashShort
    val versionOverride = AppConfig.appVersionOverride
    val result = commit + versionOverride
    logger.trace(s"AppVersion $result hash ${commit} override ${versionOverride}")
    result
  }

  lazy val componentSetFilter = new CacheFilter {
    override implicit def ec: ExecutionContext = componentSetExecutionContext.heavyLoad

    override lazy val bucket: String = AppConfig.assetsBucket

    override def appVersion: String = {
      mainAppVersion()
    }

    override def s3: AmazonS3 = bootstrap.Main.s3

    override def intercept(path: String) = path.contains("component-sets")

    override val gzipEnabled = containerConfig.componentsGzip

    override lazy val futureQueue: FutureQueuer = new BlockingFutureQueuer()
  }

  override lazy val externalModelLaunchConfig: ExternalModelLaunchConfig = ExternalModelLaunchConfig(
    org.corespring.container.client.controllers.launcher.player.routes.PlayerLauncher.playerJs().url)

  private lazy val logger = Logger(Main.getClass)

  logger.debug(s"bootstrapping... ${mainAppVersion()}")

  override lazy val controllers: Seq[Controller] = {
    super.controllers ++
      v2ApiControllers ++
      v1ApiControllers ++
      webControllers ++
      itemImportControllers ++
      developerControllers :+
      itemDraftsController
  }

  lazy val configuration = current.configuration

  lazy val containerConfig = ContainerConfig(configuration, current.mode)

  lazy val cdnResolver = new CDNResolver(
    containerConfig.cdnDomain,
    if (containerConfig.cdnAddVersionAsQueryParam) Some(mainAppVersion) else None)

  override def resolveDomain(path: String): String = cdnResolver.resolveDomain(path)

  lazy val itemAssetResolver: ItemAssetResolver = new CDNItemAssetResolver(cdnResolver)

  override lazy val elasticSearchConfig = ElasticSearchConfig(
    AppConfig.elasticSearchUrl,
    AppConfig.mongoUri,
    containerConfig.componentsPath)

  lazy val transformerItemService = new TransformerItemService(
    itemService,
    db(CollectionNames.versionedItem),
    db(CollectionNames.item))(context)

  lazy val itemTransformerConfig = ItemTransformerConfig(
    configuration.getBoolean("v2.itemTransformer.checkModelIsUpToDate").getOrElse(false))

  override lazy val sessionDbConfig: SessionDbConfig = {
    val envName = if (AppConfig.dynamoDbActivate) Some(AppConfig.envName) else None
    new SessionDbConfig(AppConfig.sessionService, AppConfig.sessionServiceUrl, AppConfig.sessionServiceAuthToken,
      envName, AppConfig.dynamoDbUseLocal, AppConfig.dynamoDbLocalInit)
  }

  override lazy val awsCredentials: AWSCredentials = new AWSCredentials {
    override lazy val getAWSAccessKeyId: String = AppConfig.amazonKey
    override lazy val getAWSSecretKey: String = AppConfig.amazonSecret
  }

  override lazy val itemTransformer: ItemTransformer = wire[AllItemVersionTransformer]

  override lazy val sessionCreatedCallback: VersionedId[ObjectId] => Unit = {
    (itemId) =>
      Actors.itemTransformerActor ! UpdateItem(itemId)
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

  override lazy val itemSessionApiExecutionContext: ItemSessionApiExecutionContext = ItemSessionApiExecutionContext(ExecutionContext.Implicits.global)

  private lazy val secureSocial: SecureSocial = new SecureSocial {}

  override lazy val userSessionOrgIdentity: UserSessionOrgIdentity[OrgAndOpts] = requestIdentifiers.userSession

  private lazy val requestIdentifiers: RequestIdentifiers = wire[RequestIdentifiers]

  override lazy val getOrgAndOptsFn: (RequestHeader) => Validation[V2Error, OrgAndOpts] = requestIdentifiers.allIdentifiers.apply

  override lazy val itemApiExecutionContext: ItemApiExecutionContext = ItemApiExecutionContext(ExecutionContext.global)

  override lazy val scoreService: ScoreService = new BasicScoreService(outcomeProcessor, scoreProcessor)(jsonFormatting.formatPlayerDefinition)

  /**
   * Note: macwire > 1.0 has a tagging option so that you can tag instances of the same type
   * in a scope so it knows which property to inject (eg if we have multiple strings in scope)
   * However this won't be available until we move to scala 2.11 and will probably involve a bump of play too.
   * In the interim we create simple types to wrap the strings
   */
  override lazy val bucket = Bucket(AppConfig.assetsBucket)

  override lazy val archiveConfig = ArchiveConfig(AppConfig.archiveContentCollectionId, AppConfig.archiveOrgId)

  override lazy val accessTokenConfig = AccessTokenConfig()

  override lazy val transferManager: TransferManager = new TransferManager(s3)

  override lazy val s3: AmazonS3 = {

    val client = new AmazonS3Client(awsCredentials)

    AppConfig.amazonEndpoint.foreach { e =>
      val options = new S3ClientOptions()
      client.setEndpoint(e)
      options.withPathStyleAccess(true)
      client.setS3ClientOptions(options)
    }

    client
  }

  override lazy val db: MongoDB = Db.salatDb()

  override lazy val context: Context = new ServicesContext(Play.classloader)

  override lazy val identifyUser: RequestHeader => Option[OrgAndUser] = (rh) => {

    def orgAndOptsToOrgAndUser(o: OrgAndOpts): OrgAndUser = OrgAndUser(
      SimpleOrg.fromOrganization(o.org),
      o.user.map(SimpleUser.fromUser))

    getOrgAndOptsFn.apply(rh).map(o => orgAndOptsToOrgAndUser(o)).toOption
  }

  override lazy val jsonFormatting: JsonFormatting = new JsonFormatting {
    override lazy val findStandardByDotNotation: (String) => Option[Standard] = standardService.findOneByDotNotation(_)

    private lazy val fieldValueLoadedOnce = fieldValueService.get.get

    override def fieldValue: FieldValue = if (Play.current.mode == Mode.Prod) {
      fieldValueLoadedOnce
    } else {
      fieldValueService.get.get
    }

    override lazy val findSubjectById: (ObjectId) => Option[Subject] = subjectService.findOneById(_)

    override lazy val rootOrgId: ObjectId = AppConfig.rootOrgId
  }

  override lazy val itemDao: VersioningDao[Item, VersionedId[ObjectId]] = {
    logger.debug(s"initializing itemDao to be ItemIndexingDao")
    new ItemIndexingDao(salatItemDao, itemIndexService, ExecutionContext.global)
  }

  def initServiceLookup() = {
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

  override def s3Service: S3Service = wire[CorespringS3ServiceExtended]

  lazy val componentLoader: ComponentLoader = {
    val path = containerConfig.componentsPath
    val showNonReleasedComponents: Boolean = containerConfig.showNonReleasedComponents
    val out = new FileComponentLoader(Seq(path), showNonReleasedComponents)
    out.reload
    out
  }

  override lazy val standardTree: StandardsTree = {
    val json: JsArray = {
      import play.api.Play.current
      Play.resourceAsStream("public/web/standards_tree.json").map { is =>
        val contents = IOUtils.toString(is, "UTF-8")
        IOUtils.closeQuietly(is)
        Json.parse(contents).as[JsArray]
      }.getOrElse(throw new RuntimeException("Can't find web/standards_tree.json"))
    }
    StandardsTree(json)
  }

  override def playMode: PlayMode = Play.current.mode

  override def appConfig: AppConfig = AppConfig

  override def containerContext: ContainerExecutionContext = new ContainerExecutionContext(ExecutionContext.global)

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
      val out = configuration.getBoolean("api.log-requests").getOrElse(Play.current.mode == Mode.Dev)
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
    val inputStream = Play.application.resourceAsStream("schema/item-schema.json")
      .getOrElse(throw new IllegalArgumentException(s"File $file not found"))
    val schema = ItemSchema(IOUtils.toString(inputStream, "UTF-8"))
    IOUtils.closeQuietly(inputStream)
    schema
  }

  override lazy val playerJsonToItem: PlayerJsonToItem = new PlayerJsonToItem(jsonFormatting)
}
