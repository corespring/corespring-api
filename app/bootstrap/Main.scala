package bootstrap

import bootstrap.Actors.UpdateItem
import com.amazonaws.auth.AWSCredentials
import com.amazonaws.services.s3.{S3ClientOptions, AmazonS3, AmazonS3Client}
import com.mongodb.casbah.MongoDB
import com.novus.salat.Context
import common.db.Db
import developer.ServiceLookup
import org.bson.types.ObjectId
import org.corespring.amazon.s3.S3Service
import org.corespring.assets.CorespringS3ServiceExtended
import org.corespring.common.config.AppConfig
import org.corespring.container.components.loader.{FileComponentLoader, ComponentLoader}
import org.corespring.drafts.item.models.OrgAndUser
import org.corespring.encryption.EncryptionModule
import org.corespring.itemSearch.{ ElasticSearchUrl, ElasticSearchExecutionContext, ItemSearchModule }
import org.corespring.models.{ Standard, Subject }
import org.corespring.models.item.{ ComponentType, FieldValue }
import org.corespring.models.json.JsonFormatting
import org.corespring.platform.data.mongo.models.VersionedId
import org.corespring.qtiToV2.transformers.{ItemTransformerConfig, ItemTransformer}
import org.corespring.services.salat.ServicesContext
import org.corespring.services.salat.bootstrap._
import org.corespring.v2.api.{ ItemSessionApiExecutionContext, ItemApiExecutionContext, V2ApiModule }
import org.corespring.v2.api.services.{BasicScoreService, ScoreService}
import org.corespring.v2.auth.{ AccessSettingsWildcardCheck, V2AuthModule }
import org.corespring.v2.auth.models.{ PlayerAccessSettings, OrgAndOpts }
import org.corespring.v2.auth.wired.{ HasPermissions, SessionServices }
import org.corespring.v2.errors.V2Error
import org.corespring.v2.player.hooks.{V2PlayerAws, CatalogAssets, PlayerAssets, StandardsTree}
import org.corespring.v2.player.{TransformerItemService, V2PlayerModule, AllItemVersionTransformer}
import play.api.Mode.{Mode => PlayMode}
import play.api.libs.json.JsArray
import play.api.{Logger, Configuration, Play}
import play.api.mvc.{ RequestHeader, Controller }

import scala.concurrent.ExecutionContext
import scalaz.Validation

object Main
  extends SalatServices
  with EncryptionModule
  with ItemSearchModule
  with V2AuthModule
  with V2ApiModule
  with V2PlayerModule {

  import com.softwaremill.macwire.MacwireMacros._

  import play.api.Play.current

  private lazy val logger = Logger(Main.getClass)

  lazy val configuration = current.configuration

  override lazy val elasticSearchUrl: ElasticSearchUrl = ElasticSearchUrl(AppConfig.elasticSearchUrl)

  override lazy val elasticSearchExecutionContext: ElasticSearchExecutionContext = ElasticSearchExecutionContext(ExecutionContext.Implicits.global)

  //session auth
  override lazy val perms: HasPermissions = new HasPermissions {
    import org.corespring.v2.auth.models.Mode
    override def has(itemId: String, sessionId: Option[String], settings: PlayerAccessSettings): Validation[V2Error, Boolean] = {
      AccessSettingsWildcardCheck.allow(itemId, sessionId, Mode.evaluate, settings)
    }
  }

  lazy val transformerItemService = new TransformerItemService(itemService,
    db("versioned_content"),
    db("content")
  )(context)

  lazy val itemTransformerConfig = ItemTransformerConfig(
    configuration.getBoolean("v2.itemTransformer.checkModelIsUpToDate").getOrElse(false)
  )

  override lazy val itemTransformer: ItemTransformer = wire[AllItemVersionTransformer]

  override lazy val sessionServices: SessionServices = ???

  override lazy val sessionCreatedCallback: VersionedId[ObjectId] => Unit = {
    (itemId) =>
      Actors.itemTransformerActor ! UpdateItem(itemId)
  }

  override lazy val componentTypes: Seq[ComponentType] = componentLoader.all.map {
    c => ComponentType(c.componentType, (c.packageInfo \ "title").asOpt[String].getOrElse(c.componentType))
  }

  override lazy val itemSessionApiExecutionContext: ItemSessionApiExecutionContext = ItemSessionApiExecutionContext(ExecutionContext.Implicits.global)

  override lazy val getOrgAndOptsFn: (RequestHeader) => Validation[V2Error, OrgAndOpts] = ???

  override lazy val itemApiExecutionContext: ItemApiExecutionContext = ItemApiExecutionContext(ExecutionContext.global)

  override lazy val scoreService: ScoreService = new BasicScoreService(outcomeProcessor, scoreProcessor)

  lazy val aws = AwsBucket(AppConfig.assetsBucket)

  lazy val archiveConfig = ArchiveConfig(AppConfig.archiveContentCollectionId, AppConfig.archiveOrgId)

  lazy val s3: AmazonS3 = {
    val client = new AmazonS3Client(new AWSCredentials {
      override lazy val getAWSAccessKeyId: String = AppConfig.amazonKey
      override lazy val getAWSSecretKey: String = AppConfig.amazonSecret
    })

    AppConfig.amazonEndpoint.foreach{ e =>
      client.setS3ClientOptions(new S3ClientOptions().withPathStyleAccess(true))
      client.setEndpoint(e)
    }
    client
  }

  lazy val accessTokenConfig = AccessTokenConfig()

  override def controllers: Seq[Controller] = Seq(itemDraftsController)

  override lazy val db: MongoDB = Db.salatDb()

  override lazy val context: Context = new ServicesContext(Play.classloader)

  override lazy val identifyUser: (RequestHeader) => Option[OrgAndUser] = ???

  override lazy val jsonFormatting: JsonFormatting = new JsonFormatting {
    override lazy val findStandardByDotNotation: (String) => Option[Standard] = standardService.findOneByDotNotation(_)

    override lazy val fieldValue: FieldValue = fieldValueService.get.get

    override lazy val findSubjectById: (ObjectId) => Option[Subject] = subjectService.findOneById(_)

    override lazy val rootOrgId: ObjectId = AppConfig.rootOrgId
  }

  def initServiceLookup() = {
    ServiceLookup.apiClientService = apiClientService
    ServiceLookup.contentCollectionService = contentCollectionService
    ServiceLookup.itemService = itemService
    ServiceLookup.jsonFormatting = jsonFormatting
    ServiceLookup.orgService = orgService
    ServiceLookup.registrationTokenService = registrationTokenService
    ServiceLookup.userService = userService
  }

  override def s3Service: S3Service = wire[CorespringS3ServiceExtended]

  override def v2PlayerAwsConfig: V2PlayerAws = V2PlayerAws(AppConfig.assetsBucket)

  override def catalogAssets: CatalogAssets = ???

  override def playerAssets: PlayerAssets = ???

  lazy val componentLoader: ComponentLoader = {
    val path = containerConfig.getString("components.path").toSeq

    val showNonReleasedComponents: Boolean = containerConfig.getBoolean("components.showNonReleasedComponents")
      .getOrElse {
        Play.current.mode == play.api.Mode.Dev
      }

    val out = new FileComponentLoader(path, showNonReleasedComponents)
    out.reload
    out
  }

  private lazy val containerConfig = {
    for {
      container <- current.configuration.getConfig("container")
      modeSpecific <- current.configuration
        .getConfig(s"container-${Play.current.mode.toString.toLowerCase}")
        .orElse(Some(Configuration.empty))
    } yield {
      val out = container ++ modeSpecific ++ current.configuration
        .getConfig("v2.auth")
        .getOrElse(Configuration.empty)
      logger.debug(s"Container config: \n${out.underlying.root.render}")
      out
    }
  }.getOrElse(Configuration.empty)


  override def standardTree: StandardsTree = {
  override val standardsTreeJson: JsArray = {
    import play.api.Play.current
    Play.resourceAsStream("public/web/standards_tree.json").map { is =>
      val contents = IOUtils.toString(is, "UTF-8")
      IOUtils.closeQuietly(is)
      Json.parse(contents).as[JsArray]
    }.getOrElse(throw new RuntimeException("Can't find web/standards_tree.json"))
  }


  override implicit def ec: ExecutionContext = ExecutionContext.Implicits.global

  override def playMode: PlayMode = Play.current.mode
}